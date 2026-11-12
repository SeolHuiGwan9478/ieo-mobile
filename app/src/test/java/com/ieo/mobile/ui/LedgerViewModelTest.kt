package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.expense.Expense
import com.ieo.mobile.domain.expense.ExpenseCategory
import com.ieo.mobile.domain.expense.ExpenseRepository
import com.ieo.mobile.domain.ledger.DailySummary
import com.ieo.mobile.domain.ledger.LedgerEntry
import com.ieo.mobile.domain.ledger.LedgerRepository
import com.ieo.mobile.domain.ledger.ManualEntry
import com.ieo.mobile.domain.ledger.Receivables
import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.ledger.FormKind
import com.ieo.mobile.ui.ledger.LedgerViewModel
import com.ieo.mobile.ui.ledger.paidTag
import com.ieo.mobile.ui.ledger.shortWon
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LedgerViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    /** 한국 2026-11-08 */
    private val clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 11, 8)

    private fun entry(id: Long, date: LocalDate = today, fare: Long = 400_000, paid: Boolean = false, overdue: Boolean = false) =
        LedgerEntry(id, true, date, "평택 → 대구", fare, PaymentType.RECEIPT, "한빛", null, null, date.plusDays(30), paid, overdue, emptyList())

    private inner class Fake : LedgerRepository, ExpenseRepository {
        var list = listOf(entry(1), entry(2, today.minusMonths(1), 300_000))
        var spent = listOf(Expense(1, today, ExpenseCategory.FUEL, 100_000, null), Expense(2, today, ExpenseCategory.FUEL, 50_000, null), Expense(3, today, ExpenseCategory.MEAL, 200_000, null))
        var result: ApiResult<LedgerEntry> = ApiResult.Success(entry(1))
        var range: Pair<LocalDate, LocalDate>? = null
        val calls = mutableListOf<String>()
        var manual: ManualEntry? = null
        var updated: Pair<Long, PaymentType>? = null
        var updatedDistance: Int? = null
        /** 베이직: 이번 달보다 앞선 기간을 달라고 하면 서버가 PLAN_403_UPGRADE */
        var basic = false
        override suspend fun today() = ApiResult.Success(DailySummary(this@LedgerViewModelTest.today, 0, 0))
        override suspend fun export(from: java.time.LocalDate, to: java.time.LocalDate): ApiResult<ByteArray> = ApiResult.Success(byteArrayOf(1, 2))
        override suspend fun receivables() = ApiResult.Success(Receivables(400_000, 0, 30_000, listOf(entry(1))))
        override suspend fun entries(from: LocalDate, to: LocalDate): ApiResult<List<LedgerEntry>> {
            range = from to to
            if (basic && from.isBefore(today.withDayOfMonth(1))) {
                return ApiResult.Failure(com.ieo.mobile.core.network.ApiError.Server(403, "PLAN_403_UPGRADE", "잠김"))
            }
            return ApiResult.Success(list.filter { !basic || !it.workDate.isBefore(from) })
        }
        override suspend fun setPaid(entryId: Long, paid: Boolean): ApiResult<LedgerEntry> {
            calls += "paid$entryId:$paid"
            return result
        }
        override suspend fun setExtraIncomePaid(extraIncomeId: Long, paid: Boolean) = ApiResult.Success(Unit)
        override suspend fun update(entry: LedgerEntry, fare: Long, paymentType: PaymentType, distanceKm: Int?): ApiResult<LedgerEntry> {
            updated = fare to paymentType
            updatedDistance = distanceKm
            return ApiResult.Success(entry)
        }
        override suspend fun addManual(entry: ManualEntry): ApiResult<LedgerEntry> {
            manual = entry
            return ApiResult.Success(entry(9))
        }
        override suspend fun expenses(from: LocalDate, to: LocalDate) = ApiResult.Success(spent)
        override suspend fun add(category: ExpenseCategory, amount: Long, memo: String?): ApiResult<Expense> {
            calls += "expense:$category:$amount:$memo"
            return ApiResult.Success(Expense(9, today, category, amount, memo))
        }
        override suspend fun delete(expenseId: Long): ApiResult<Unit> {
            calls += "delete expense $expenseId"
            spent = spent.filter { it.id != expenseId }
            return ApiResult.Success(Unit)
        }
        override suspend fun taxInvoice(entryId: Long): ApiResult<com.ieo.mobile.domain.ledger.TaxInvoice> = ApiResult.Failure(com.ieo.mobile.core.network.ApiError.Unexpected)
        override suspend fun issueTaxInvoice(entryId: Long): ApiResult<com.ieo.mobile.domain.ledger.TaxInvoice> = ApiResult.Failure(com.ieo.mobile.core.network.ApiError.Unexpected)
        override suspend fun deleteEntry(entryId: Long): ApiResult<Unit> {
            calls += "delete entry $entryId"
            list = list.filter { it.id != entryId }
            return ApiResult.Success(Unit)
        }
    }

    /** 테스트에서는 PDF를 그리지 않고 받은 내용만 남긴다 */
    private var rendered: com.ieo.mobile.domain.ledger.LedgerReport? = null

    private fun vm(fake: Fake) = LedgerViewModel(fake, fake, clock, renderPdf = { report -> rendered = report; byteArrayOf(0x25, 0x50) }, pdfDispatcher = main.dispatcher)

    @Test
    fun `베이직이면 이번 달만 받고 지난 달과 내보내기를 잠근다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { basic = true }
        val vm = vm(fake)
        advanceUntilIdle()
        assertTrue(vm.state.value.historyLocked)
        assertFalse(vm.state.value.loadFailed)
        assertEquals(today.withDayOfMonth(1), fake.range?.first)
        assertEquals(1, vm.state.value.entries.size)
        vm.openExport()
        assertFalse(vm.state.value.choosingExport)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `작년 장부를 받으면 파일을 넘기고 넘긴 뒤 지운다`() = runTest(main.dispatcher) {
        val vm = vm(Fake())
        advanceUntilIdle()
        val lastYear = vm.state.value.currentMonth.year - 1
        vm.chooseExportFormat(com.ieo.mobile.ui.ledger.ExportFormat.CSV)
        vm.export(com.ieo.mobile.ui.ledger.ExportPeriod.LAST_YEAR)
        advanceUntilIdle()
        assertEquals("이에오_장부_$lastYear-01-01_$lastYear-12-31.csv", vm.state.value.exported?.fileName)
        assertEquals("text/csv", vm.state.value.exported?.mimeType)
        vm.exportConsumed()
        assertEquals(null, vm.state.value.exported)
    }

    @Test
    fun `PDF는 그 기간 운행과 경비를 받아 휴대폰에서 만든다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = vm(fake)
        advanceUntilIdle()
        assertEquals(com.ieo.mobile.ui.ledger.ExportFormat.PDF, vm.state.value.exportFormat)
        vm.export(com.ieo.mobile.ui.ledger.ExportPeriod.MONTH)
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 11, 1) to LocalDate.of(2026, 11, 30), fake.range)
        assertEquals("이에오_장부_2026-11.pdf", vm.state.value.exported?.fileName)
        assertEquals("application/pdf", vm.state.value.exported?.mimeType)
        assertEquals("2026년 11월", rendered?.periodLabel)
        assertEquals(350_000L, rendered?.expenseTotal)
        assertFalse(vm.state.value.isBusy)
    }

    @Test
    fun `이 달 실수익은 매출에서 경비를 뺀다`() = runTest(main.dispatcher) {
        val vm = vm(Fake())
        advanceUntilIdle()
        assertEquals(50_000L, vm.state.value.ledger.netProfit)
        assertEquals(430_000L, vm.state.value.owedTotal)
    }

    @Test
    fun `서버 한도 안에서 12개월을 받는다`() = runTest(main.dispatcher) {
        val fake = Fake()
        vm(fake)
        advanceUntilIdle()
        assertEquals(LocalDate.of(2025, 12, 1) to LocalDate.of(2026, 11, 30), fake.range)
    }

    @Test
    fun `달을 옮길 수 있는 범위를 지킨다`() = runTest(main.dispatcher) {
        val vm = vm(Fake())
        advanceUntilIdle()
        vm.nextMonth()
        assertEquals(YearMonth.of(2026, 11), vm.state.value.month)
        repeat(20) { vm.previousMonth() }
        assertEquals(YearMonth.of(2025, 12), vm.state.value.month)
        assertFalse(vm.state.value.canGoBack)
    }

    @Test
    fun `그래프는 최근 6개월이다`() = runTest(main.dispatcher) {
        val vm = vm(Fake())
        advanceUntilIdle()
        val history = vm.state.value.history
        assertEquals(6, history.size)
        assertEquals(YearMonth.of(2026, 6), history.first().month)
        assertEquals(300_000L, history[4].net)
    }

    @Test
    fun `경비는 분류별로 큰 순서로 묶는다`() = runTest(main.dispatcher) {
        val vm = vm(Fake())
        advanceUntilIdle()
        assertEquals(listOf(ExpenseCategory.MEAL to 200_000L, ExpenseCategory.FUEL to 150_000L), vm.state.value.expenseByCategory)
    }

    @Test
    fun `받음으로 표시하고 되돌릴 수 있다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = vm(fake)
        advanceUntilIdle()
        vm.setPaid(entry(1), true)
        advanceUntilIdle()
        val undo = vm.state.value.notice?.undo
        assertNotNull(undo)
        undo!!.invoke()
        advanceUntilIdle()
        assertEquals(listOf("paid1:true", "paid1:false"), fake.calls)
        assertNull(vm.state.value.notice?.undo)
    }

    @Test
    fun `받음 표시가 실패하면 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { result = ApiResult.Failure(ApiError.Network) }
        val vm = vm(fake)
        advanceUntilIdle()
        vm.setPaid(entry(1), true)
        advanceUntilIdle()
        assertEquals(ApiError.Network.message, vm.state.value.error)
        assertNull(vm.state.value.notice)
    }

    @Test
    fun `경비는 금액이 있어야 저장하고 빠른 버튼으로 더한다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = vm(fake)
        advanceUntilIdle()
        vm.openExpense()
        assertFalse(vm.state.value.form!!.canSave)
        vm.addAmount(50_000)
        vm.addAmount(10_000)
        vm.editForm { it.copy(category = ExpenseCategory.TOLL, memo = "  ") }
        vm.saveForm()
        advanceUntilIdle()
        assertEquals("expense:TOLL:60000:null", fake.calls.single())
        assertNull(vm.state.value.form)
    }

    @Test
    fun `직접 추가는 구간이 있어야 하고 어제를 고를 수 있다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = vm(fake)
        advanceUntilIdle()
        vm.openManual()
        vm.onAmount("300000")
        assertFalse(vm.state.value.form!!.canSave)
        vm.editForm { it.copy(description = "평택 → 대구", yesterday = true, distance = "312") }
        vm.saveForm()
        advanceUntilIdle()
        assertEquals(today.minusDays(1), fake.manual?.workDate)
        assertEquals(312, fake.manual?.distanceKm)
    }

    @Test
    fun `운임 고치기는 지금 값으로 시작한다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = vm(fake)
        advanceUntilIdle()
        vm.openEntry(fake.list.first())
        vm.openEditFare()
        assertEquals(FormKind.EDIT_FARE, vm.state.value.form?.kind)
        assertEquals("400000", vm.state.value.form?.amount)
        assertEquals("", vm.state.value.form?.distance)
        vm.onAmount("420000")
        vm.editForm { it.copy(distance = "305") }
        vm.saveForm()
        advanceUntilIdle()
        assertEquals(420_000L to PaymentType.RECEIPT, fake.updated)
        assertEquals(305, fake.updatedDistance)
    }

    @Test
    fun `뒤로 가면 위에 열린 화면부터 닫는다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = vm(fake)
        advanceUntilIdle()
        vm.openEntry(fake.list.first())
        vm.openEditFare()
        vm.back()
        assertNull(vm.state.value.form)
        assertNotNull(vm.state.value.selected)
        vm.back()
        assertFalse(vm.state.value.hasOverlay)
    }

    @Test
    fun `받음 상태는 글자로 구분한다`() {
        assertEquals("받음", paidTag(entry(1, paid = true)).first)
        assertEquals("예정일 지남", paidTag(entry(1, overdue = true)).first)
        assertEquals("못 받음", paidTag(entry(1)).first)
        assertEquals("412만", shortWon(4_120_000))
        assertTrue(shortWon(0) == "0만")
    }

    @Test
    fun `잘못 넣은 경비는 묻고 나서 지운다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = vm(fake)
        advanceUntilIdle()
        val fuel = fake.spent.first()
        vm.askDelete(fuel)
        assertEquals(com.ieo.mobile.ui.ledger.Deletion.OfExpense(fuel), vm.state.value.deleting)
        vm.cancelDelete()
        assertNull(vm.state.value.deleting)
        assertTrue(fake.calls.none { it.startsWith("delete") })

        vm.askDelete(fuel)
        vm.confirmDelete()
        advanceUntilIdle()
        assertTrue("delete expense 1" in fake.calls)
        assertEquals("주유 경비를 지웠어요", vm.state.value.notice?.text)
        assertTrue(vm.state.value.expenses.none { it.id == 1L })
    }

    @Test
    fun `직접 추가한 운행만 지우고 지우면 상세를 닫는다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val manual = entry(5).copy(auto = false)
        fake.list = fake.list + manual
        val vm = vm(fake)
        advanceUntilIdle()

        vm.askDelete(fake.list.first())
        assertNull(vm.state.value.deleting)

        vm.openEntry(manual)
        vm.askDelete(manual)
        vm.confirmDelete()
        advanceUntilIdle()
        assertTrue("delete entry 5" in fake.calls)
        assertNull(vm.state.value.selectedEntryId)
        assertEquals("운행을 지웠어요", vm.state.value.notice?.text)
    }
}
