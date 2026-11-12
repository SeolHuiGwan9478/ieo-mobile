package com.ieo.mobile.domain

import com.ieo.mobile.domain.expense.Expense
import com.ieo.mobile.domain.expense.ExpenseCategory
import com.ieo.mobile.domain.ledger.ExtraIncome
import com.ieo.mobile.domain.ledger.LedgerEntry
import com.ieo.mobile.domain.ledger.LedgerReport
import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.domain.trip.StopType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerReportTest {

    private val nov = LocalDate.of(2026, 11, 1)

    private fun entry(id: Long, day: Int, fare: Long, paid: Boolean, waitFee: Long = 0, waitPaid: Boolean = false) = LedgerEntry(
        id, true, nov.withDayOfMonth(day), "평택 → 대구", fare, PaymentType.RECEIPT, "한빛물류", null, null, null, paid, false,
        if (waitFee > 0) listOf(ExtraIncome(id, StopType.PICKUP, "평택", waitFee, waitPaid)) else emptyList(),
    )

    @Test
    fun `합계와 못 받은 돈과 분류별 경비를 낸다`() {
        val report = LedgerReport(
            nov, nov.withDayOfMonth(30),
            listOf(entry(2, 20, 450_000, paid = false, waitFee = 10_000), entry(1, 3, 300_000, paid = true, waitFee = 20_000, waitPaid = true)),
            listOf(Expense(1, nov.withDayOfMonth(5), ExpenseCategory.MEAL, 9_000, null), Expense(2, nov.withDayOfMonth(4), ExpenseCategory.FUEL, 150_000, "가득")),
            madeOn = LocalDate.of(2026, 11, 12),
        )
        assertEquals(listOf(1L, 2L), report.entries.map { it.id })
        assertEquals(listOf(2L, 1L), report.expenses.map { it.id })
        assertEquals(750_000L, report.fareTotal)
        assertEquals(30_000L, report.waitFeeTotal)
        assertEquals(159_000L, report.expenseTotal)
        assertEquals(621_000L, report.netProfit)
        assertEquals(450_000L, report.unpaidFare)
        assertEquals(10_000L, report.unpaidWaitFee)
        assertEquals(listOf(ExpenseCategory.FUEL to 150_000L, ExpenseCategory.MEAL to 9_000L), report.expenseByCategory)
    }

    @Test
    fun `기간 이름은 한 달, 한 해, 그 밖으로 나눈다`() {
        val made = LocalDate.of(2026, 11, 12)
        assertEquals("2026년 11월", LedgerReport(nov, nov.withDayOfMonth(30), emptyList(), emptyList(), made).periodLabel)
        assertEquals("2025년", LedgerReport(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), emptyList(), emptyList(), made).periodLabel)
        val custom = LedgerReport(nov, nov.withDayOfMonth(15), emptyList(), emptyList(), made)
        assertEquals("2026.11.01 ~ 2026.11.15", custom.periodLabel)
        assertEquals("2026-11-01_2026-11-15", custom.fileLabel)
        assertTrue(custom.isEmpty)
    }
}
