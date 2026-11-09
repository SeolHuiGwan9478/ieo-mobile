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
        override suspend fun today() = ApiResult.Success(DailySummary(this@LedgerViewModelTest.today, 0, 0))
        override suspend fun export(from: java.time.LocalDate, to: java.time.LocalDate): ApiResult<ByteArray> = ApiResult.Success(byteArrayOf(1, 2))
        override suspend fun receivables() = ApiResult.Success(Receivables(400_000, 0, 30_000, listOf(entry(1))))
        override suspend fun entries(from: LocalDate, to: LocalDate): ApiResult<List<LedgerEntry>> {
            range = from to to
            return ApiResult.Success(list)
        }
        override suspend fun setPaid(entryId: Long, paid: Boolean): ApiResult<LedgerEntry> {
            calls += "paid$entryId:$paid"
            return result
        }
        override suspend fun setExtraIncomePaid(extraIncomeId: Long, paid: Boolean) = ApiResult.Success(Unit)
        override suspend fun update(entry: LedgerEntry, fare: Long, paymentType: PaymentType): ApiResult<LedgerEntry> {
            updated = fare to paymentType
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
    }

    private fun vm(fake: Fake) = LedgerViewModel(fake, fake, clock)

    @Test
    fun `작년 장부를 받으면 파일을 넘기고 넘긴 뒤 지운다`() = runTest(main.dispatcher) {
        val vm = vm(Fake())
        advanceUntilIdle()
        val lastYear = vm.state.value.currentMonth.year - 1
        vm.export(com.ieo.mobile.ui.ledger.ExportPeriod.LAST_YEAR)
        advanceUntilIdle()
        assertEquals("이에오_장부_$lastYear-01-01_$lastYear-12-31.csv", vm.state.value.exported?.fileName)
        vm.exportConsumed()
        assertEquals(null, vm.state.value.exported)
    }

    @Test
    fun `이 달 순수익은 매출에서 경비를 뺀다`() = runTest(main.dispatcher) {
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
        vm.editForm { it.copy(description = "평택 → 대구", yesterday = true) }
        vm.saveForm()
        advanceUntilIdle()
        assertEquals(today.minusDays(1), fake.manual?.workDate)
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
        vm.onAmount("420000")
        vm.saveForm()
        advanceUntilIdle()
        assertEquals(420_000L to PaymentType.RECEIPT, fake.updated)
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
}
