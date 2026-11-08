package com.ieo.mobile.domain.ledger

import com.ieo.mobile.domain.expense.Expense
import java.time.LocalDate
import java.time.YearMonth

/**
 * 한 달 장부: 운행 줄과 경비를 합쳐 요약하고 날짜별로 묶는다.
 * 월 요약 API 없이 앱에서 계산한다 (서버 월별 리포트 LEDG-032는 프로 플랜에서).
 */
class MonthlyLedger(
    val month: YearMonth,
    private val entries: List<LedgerEntry>,
    private val expenses: List<Expense>,
) {

    /** 운임 + 대기비 (받았든 아니든) */
    val revenue: Long get() = entries.sumOf { it.total }

    val expenseTotal: Long get() = expenses.sumOf { it.amount }

    val netProfit: Long get() = revenue - expenseTotal

    /** 날짜별 묶음, 최근 날짜부터. 하루 안에서는 운행 줄이 먼저 */
    fun days(): List<LedgerDay> {
        val dates = (entries.map { it.workDate } + expenses.map { it.spentOn }).distinct().sortedDescending()
        return dates.map { date ->
            LedgerDay(
                date = date,
                entries = entries.filter { it.workDate == date },
                expenses = expenses.filter { it.spentOn == date },
            )
        }
    }
}

data class LedgerDay(val date: LocalDate, val entries: List<LedgerEntry>, val expenses: List<Expense>)
