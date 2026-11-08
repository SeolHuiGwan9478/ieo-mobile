package com.ieo.mobile.domain.expense

import com.ieo.mobile.core.network.ApiResult
import java.time.LocalDate

/** 경비 한 건 (LEDG-010) */
data class Expense(
    val id: Long,
    val spentOn: LocalDate,
    val category: ExpenseCategory,
    val amount: Long,
    val memo: String?,
)

/** 경비 분류 (LEDG-011) */
enum class ExpenseCategory(val label: String) {
    FUEL("주유"),
    TOLL("통행료"),
    MEAL("식대"),
    MAINTENANCE("정비"),
    INSURANCE("보험"),
    PARKING("주차"),
    ETC("기타"),
}

interface ExpenseRepository {

    suspend fun expenses(from: LocalDate, to: LocalDate): ApiResult<List<Expense>>

    /** 오늘 쓴 경비. 진행 중인 운행이 있으면 서버가 그 운행에 붙인다 */
    suspend fun add(category: ExpenseCategory, amount: Long, memo: String?): ApiResult<Expense>
}
