package com.ieo.mobile.domain.ledger

import com.ieo.mobile.domain.expense.Expense
import com.ieo.mobile.domain.expense.ExpenseCategory
import java.time.LocalDate
import java.time.YearMonth

/**
 * PDF 장부에 넣을 내용 (LEDG-033). 고른 기간의 운행 줄과 경비를 날짜순으로 모으고 합계를 낸다.
 * 화면 그리기는 [com.ieo.mobile.ui.ledger.LedgerPdf]가 한다
 */
class LedgerReport(
    val from: LocalDate,
    val to: LocalDate,
    entries: List<LedgerEntry>,
    expenses: List<Expense>,
    /** 만든 날 (한국 날짜) */
    val madeOn: LocalDate,
    /** 기사님 이름. 모르면 null */
    val driverName: String? = null,
) {
    val entries: List<LedgerEntry> = entries.sortedWith(compareBy({ it.workDate }, { it.id }))
    val expenses: List<Expense> = expenses.sortedWith(compareBy({ it.spentOn }, { it.id }))

    val tripCount: Int get() = entries.size
    val fareTotal: Long get() = entries.sumOf { it.fare }
    val waitFeeTotal: Long get() = entries.sumOf { entry -> entry.extraIncomes.sumOf { it.amount } }

    /** 운임 + 대기비 (받았든 아니든) */
    val revenue: Long get() = fareTotal + waitFeeTotal
    val expenseTotal: Long get() = expenses.sumOf { it.amount }
    val netProfit: Long get() = revenue - expenseTotal

    /** 이 기간 운행 중 아직 받지 못한 운임과 대기비 */
    val unpaidFare: Long get() = entries.filterNot { it.paid }.sumOf { it.fare }
    val unpaidWaitFee: Long get() = entries.sumOf { entry -> entry.extraIncomes.filterNot { it.paid }.sumOf { it.amount } }

    /** 분류별 경비. 쓴 분류만, 분류 순서대로 */
    val expenseByCategory: List<Pair<ExpenseCategory, Long>>
        get() = ExpenseCategory.entries
            .map { category -> category to expenses.filter { it.category == category }.sumOf { it.amount } }
            .filter { it.second > 0 }

    val isEmpty: Boolean get() = entries.isEmpty() && expenses.isEmpty()

    /** "2026년 11월", "2026년", 그 밖에는 "2026.11.01 ~ 2026.11.15" */
    val periodLabel: String
        get() {
            val month = YearMonth.from(from)
            return when {
                from == month.atDay(1) && to == month.atEndOfMonth() -> "${month.year}년 ${month.monthValue}월"
                from.dayOfYear == 1 && to == from.withDayOfYear(from.lengthOfYear()) -> "${from.year}년"
                else -> "${dotted(from)} ~ ${dotted(to)}"
            }
        }

    /** 파일 이름에 쓰는 기간 "2026-11" 또는 "2026" 또는 "2026-11-01_2026-11-15" */
    val fileLabel: String
        get() {
            val month = YearMonth.from(from)
            return when {
                from == month.atDay(1) && to == month.atEndOfMonth() -> month.toString()
                from.dayOfYear == 1 && to == from.withDayOfYear(from.lengthOfYear()) -> from.year.toString()
                else -> "${from}_$to"
            }
        }

    companion object {
        fun dotted(day: LocalDate): String = "%d.%02d.%02d".format(day.year, day.monthValue, day.dayOfMonth)
    }
}
