package com.ieo.mobile.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.map
import com.ieo.mobile.domain.expense.Expense
import com.ieo.mobile.domain.expense.ExpenseCategory
import com.ieo.mobile.domain.expense.ExpenseRepository
import com.ieo.mobile.domain.ledger.ExtraIncome
import com.ieo.mobile.domain.ledger.LedgerEntry
import com.ieo.mobile.domain.ledger.LedgerReport
import com.ieo.mobile.domain.ledger.LedgerRepository
import com.ieo.mobile.domain.ledger.ManualEntry
import com.ieo.mobile.domain.ledger.MonthlyLedger
import com.ieo.mobile.domain.ledger.Receivables
import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.ui.common.DayTimeFormatter
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 입력 창 종류 */
enum class FormKind { EXPENSE, MANUAL, EDIT_FARE }

data class LedgerForm(
    val kind: FormKind,
    val amount: String = "",
    val category: ExpenseCategory = ExpenseCategory.FUEL,
    val memo: String = "",
    val yesterday: Boolean = false,
    val description: String = "",
    val paymentType: PaymentType = PaymentType.RECEIPT,
    val brokerName: String = "",
    /** 주행거리(km). 비우면 모르는 것으로 */
    val distance: String = "",
) {
    val distanceValue: Int? get() = distance.toIntOrNull()?.takeIf { it > 0 }
    val amountValue: Long get() = amount.toLongOrNull() ?: 0
    val canSave: Boolean
        get() = amountValue > 0 && (kind != FormKind.MANUAL || description.isNotBlank())
}

/** 저장 뒤 알림. [undo]가 있으면 되돌리기를 보여 준다 */
/** 내려받은 장부 파일 */
class ExportedLedger(val fileName: String, val bytes: ByteArray, val mimeType: String)

/** 내보내기 형식: 세무 신고용 엑셀(CSV)은 서버가, 보관·전달용 PDF는 휴대폰이 만든다 */
enum class ExportFormat(val label: String) { PDF("PDF"), CSV("엑셀(CSV)") }

enum class ExportPeriod(val label: String) { MONTH("보고 있는 달"), THIS_YEAR("올해"), LAST_YEAR("작년") }

data class LedgerNotice(val text: String, val undo: (() -> Unit)? = null)

/** 지우기 전에 묻는 것: 경비 한 건 또는 직접 추가한 운행 줄 */
sealed interface Deletion {
    data class OfExpense(val expense: Expense) : Deletion
    data class OfEntry(val entry: LedgerEntry) : Deletion
}

/** 그래프의 한 달 */
data class MonthPoint(val month: YearMonth, val net: Long)

data class LedgerState(
    val month: YearMonth,
    val currentMonth: YearMonth,
    val entries: List<LedgerEntry> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val receivables: Receivables? = null,
    val selectedEntryId: Long? = null,
    val showingReceivables: Boolean = false,
    val form: LedgerForm? = null,
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val loadFailed: Boolean = false,
    val error: String? = null,
    val notice: LedgerNotice? = null,
    val choosingExport: Boolean = false,
    /** "지울까요?" 창. 닫으면 null */
    val deleting: Deletion? = null,
    /** 내보내기 창에서 고른 형식 */
    val exportFormat: ExportFormat = ExportFormat.PDF,
    /** 받아 온 파일. 화면이 공유 창을 열고 지운다 */
    val exported: ExportedLedger? = null,
) {
    val firstMonth: YearMonth get() = currentMonth.minusMonths(MONTHS_KEPT - 1)
    val canGoBack: Boolean get() = month.isAfter(firstMonth)
    val canGoForward: Boolean get() = month.isBefore(currentMonth)

    val ledger: MonthlyLedger get() = ledgerOf(month)

    /** 최근 6개월 실수익 */
    val history: List<MonthPoint>
        get() = (CHART_MONTHS - 1 downTo 0L).map { currentMonth.minusMonths(it) }.map { MonthPoint(it, ledgerOf(it).netProfit) }

    /** 이 달 경비를 분류별로, 큰 순서로 */
    val expenseByCategory: List<Pair<ExpenseCategory, Long>>
        get() = expenses.filter { YearMonth.from(it.spentOn) == month }
            .groupBy { it.category }
            .map { (category, list) -> category to list.sumOf { it.amount } }
            .sortedByDescending { it.second }

    val selected: LedgerEntry?
        get() = selectedEntryId?.let { id ->
            entries.firstOrNull { it.id == id } ?: receivables?.entries?.firstOrNull { it.id == id }
        }

    val hasOverlay: Boolean get() = selectedEntryId != null || showingReceivables || form != null

    /** 받지 못한 돈 전체 */
    val owedTotal: Long get() = receivables?.let { it.fareTotal + it.waitFeeTotal } ?: 0

    private fun ledgerOf(target: YearMonth) = MonthlyLedger(
        target,
        entries.filter { YearMonth.from(it.workDate) == target },
        expenses.filter { YearMonth.from(it.spentOn) == target },
    )

    companion object {
        /** 서버는 한 번에 366일까지 준다 */
        const val MONTHS_KEPT = 12L
        const val CHART_MONTHS = 6L
    }
}

/** 수익·장부 (LEDG, 시안 M10): 월 요약, 날짜별 장부, 못 받은 돈, 받음 표시, 경비 */
class LedgerViewModel(
    private val ledger: LedgerRepository,
    private val expenses: ExpenseRepository,
    private val clock: Clock = Clock.system(DayTimeFormatter.KOREA),
    /** PDF 그리기. 테스트에서는 바꿔 끼운다 */
    private val renderPdf: (LedgerReport) -> ByteArray = LedgerPdf::render,
    /** PDF를 그리는 곳. 화면이 멈추지 않게 기본은 계산용 스레드 */
    private val pdfDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private fun today(): LocalDate = clock.instant().atZone(DayTimeFormatter.KOREA).toLocalDate()

    private val _state = MutableStateFlow(YearMonth.from(today()).let { LedgerState(month = it, currentMonth = it) })
    val state: StateFlow<LedgerState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            reload()
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun previousMonth() = _state.update { if (it.canGoBack) it.copy(month = it.month.minusMonths(1)) else it }

    fun nextMonth() = _state.update { if (it.canGoForward) it.copy(month = it.month.plusMonths(1)) else it }

    fun selectMonth(month: YearMonth) = _state.update { it.copy(month = month) }

    fun openEntry(entry: LedgerEntry) = _state.update { it.copy(selectedEntryId = entry.id, error = null) }

    fun openReceivables() = _state.update { it.copy(showingReceivables = true, error = null) }

    fun openExpense() = openForm(LedgerForm(FormKind.EXPENSE))

    fun openManual() = openForm(LedgerForm(FormKind.MANUAL))

    fun openEditFare() {
        val entry = _state.value.selected ?: return
        openForm(LedgerForm(FormKind.EDIT_FARE, amount = entry.fare.toString(), paymentType = entry.paymentType, distance = entry.distanceKm?.toString().orEmpty()))
    }

    private fun openForm(form: LedgerForm) = _state.update { it.copy(form = form, error = null) }

    /** 위에 열린 화면을 하나 닫는다 */
    fun back() = _state.update {
        when {
            it.form != null -> it.copy(form = null, error = null)
            it.selectedEntryId != null -> it.copy(selectedEntryId = null, error = null)
            else -> it.copy(showingReceivables = false, error = null)
        }
    }

    fun editForm(change: (LedgerForm) -> LedgerForm) = _state.update { it.copy(form = it.form?.let(change), error = null) }

    fun onAmount(value: String) = editForm { it.copy(amount = value.filter(Char::isDigit).take(9)) }

    fun addAmount(won: Long) = editForm { it.copy(amount = (it.amountValue + won).toString()) }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun askDelete(expense: Expense) = _state.update { it.copy(deleting = Deletion.OfExpense(expense), error = null) }

    /** 운행이 끝나 생긴 줄은 지울 수 없다 (서버도 거절한다) */
    fun askDelete(entry: LedgerEntry) {
        if (entry.auto) return
        _state.update { it.copy(deleting = Deletion.OfEntry(entry), error = null) }
    }

    fun cancelDelete() = _state.update { it.copy(deleting = null) }

    /** 지운다. 운행 줄이면 상세도 닫는다 */
    fun confirmDelete() {
        val target = _state.value.deleting ?: return
        _state.update { it.copy(deleting = null) }
        when (target) {
            is Deletion.OfExpense -> change(notice = LedgerNotice("${target.expense.category.label} 경비를 지웠어요")) {
                expenses.delete(target.expense.id)
            }
            is Deletion.OfEntry -> change(notice = LedgerNotice("운행을 지웠어요")) {
                ledger.deleteEntry(target.entry.id).also { result ->
                    if (result is ApiResult.Success) _state.update { it.copy(selectedEntryId = null) }
                }
            }
        }
    }

    fun setPaid(entry: LedgerEntry, paid: Boolean): Unit = change(
        block = { ledger.setPaid(entry.id, paid).toUnit() },
        notice = if (paid) LedgerNotice("받음으로 표시했어요") { setPaid(entry, false) } else LedgerNotice("못 받음으로 되돌렸어요"),
    )

    fun setExtraPaid(extra: ExtraIncome, paid: Boolean): Unit = change(
        block = { ledger.setExtraIncomePaid(extra.id, paid) },
        notice = if (paid) LedgerNotice("대기비를 받음으로 표시했어요") { setExtraPaid(extra, false) } else LedgerNotice("못 받음으로 되돌렸어요"),
    )

    fun saveForm() {
        val form = _state.value.form?.takeIf { it.canSave } ?: return
        val entry = _state.value.selected
        change(closeForm = true, notice = LedgerNotice("저장했어요"), block = {
            when (form.kind) {
                FormKind.EXPENSE -> expenses.add(form.category, form.amountValue, form.memo.trim().ifEmpty { null }).toUnit()
                FormKind.MANUAL -> ledger.addManual(
                    ManualEntry(
                        workDate = if (form.yesterday) today().minusDays(1) else today(),
                        description = form.description.trim(),
                        fare = form.amountValue,
                        paymentType = form.paymentType,
                        brokerName = form.brokerName.trim().ifEmpty { null },
                        distanceKm = form.distanceValue,
                    ),
                ).toUnit()
                FormKind.EDIT_FARE -> ledger.update(entry!!, form.amountValue, form.paymentType, form.distanceValue).toUnit()
            }
        })
    }

    private fun <T> ApiResult<T>.toUnit(): ApiResult<Unit> = when (this) {
        is ApiResult.Success -> ApiResult.Success(Unit)
        is ApiResult.Failure -> this
    }

    fun openExport() = _state.update { it.copy(choosingExport = true, error = null) }

    fun chooseExportFormat(format: ExportFormat) = _state.update { it.copy(exportFormat = format) }

    fun closeExport() = _state.update { it.copy(choosingExport = false) }

    fun exportConsumed() = _state.update { it.copy(exported = null) }

    /** 고른 기간의 장부를 고른 형식으로 만든다 (LEDG-033). 다 만들면 화면이 공유 창을 연다 */
    fun export(period: ExportPeriod) {
        val current = _state.value
        if (current.isBusy) return
        val (from, to) = when (period) {
            ExportPeriod.MONTH -> current.month.atDay(1) to current.month.atEndOfMonth()
            ExportPeriod.THIS_YEAR -> LocalDate.of(current.currentMonth.year, 1, 1) to LocalDate.of(current.currentMonth.year, 12, 31)
            ExportPeriod.LAST_YEAR -> LocalDate.of(current.currentMonth.year - 1, 1, 1) to LocalDate.of(current.currentMonth.year - 1, 12, 31)
        }
        _state.update { it.copy(isBusy = true, choosingExport = false, error = null, notice = null) }
        viewModelScope.launch {
            val result = when (current.exportFormat) {
                ExportFormat.CSV -> ledger.export(from, to).map { ExportedLedger("이에오_장부_${from}_$to.csv", it, "text/csv") }
                ExportFormat.PDF -> pdf(from, to)
            }
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(exported = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            }
            _state.update { it.copy(isBusy = false) }
        }
    }

    /** 그 기간의 운행 줄과 경비를 받아 휴대폰에서 PDF로 그린다 */
    private suspend fun pdf(from: LocalDate, to: LocalDate): ApiResult<ExportedLedger> {
        val entries = when (val result = ledger.entries(from, to)) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> return result
        }
        val spent = when (val result = expenses.expenses(from, to)) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> return result
        }
        val report = LedgerReport(from, to, entries, spent, today())
        val bytes = withContext(pdfDispatcher) { renderPdf(report) }
        return ApiResult.Success(ExportedLedger("이에오_장부_${report.fileLabel}.pdf", bytes, "application/pdf"))
    }

    private fun change(closeForm: Boolean = false, notice: LedgerNotice, block: suspend () -> ApiResult<Unit>) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, error = null, notice = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(form = if (closeForm) null else it.form, notice = notice) }
                    reload()
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            }
            _state.update { it.copy(isBusy = false) }
        }
    }

    private suspend fun reload() {
        val current = _state.value
        val from = current.firstMonth.atDay(1)
        val to = current.currentMonth.atEndOfMonth()
        val entries = ledger.entries(from, to)
        val spent = expenses.expenses(from, to)
        val owed = ledger.receivables()
        val failure = listOf(entries, spent, owed).filterIsInstance<ApiResult.Failure>().firstOrNull()
        _state.update {
            it.copy(
                entries = (entries as? ApiResult.Success)?.data ?: it.entries,
                expenses = (spent as? ApiResult.Success)?.data ?: it.expenses,
                receivables = (owed as? ApiResult.Success)?.data ?: it.receivables,
                loadFailed = failure != null,
                error = failure?.error?.message,
            )
        }
    }
}
