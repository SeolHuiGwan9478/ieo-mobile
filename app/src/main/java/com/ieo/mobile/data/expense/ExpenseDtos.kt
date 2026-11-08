package com.ieo.mobile.data.expense

import com.ieo.mobile.domain.expense.Expense
import com.ieo.mobile.domain.expense.ExpenseCategory
import java.time.LocalDate
import kotlinx.serialization.Serializable

// 서버 domain/ledger/dto의 경비와 같은 모양

@Serializable
data class ExpenseResponse(
    val id: Long,
    val spentOn: String,
    val category: String,
    val amount: Long,
    val memo: String? = null,
) {
    fun toDomain() = Expense(
        id = id,
        spentOn = LocalDate.parse(spentOn),
        category = ExpenseCategory.valueOf(category),
        amount = amount,
        memo = memo?.takeIf { it.isNotBlank() },
    )
}

/** 날짜를 비우면 서버의 오늘 */
@Serializable
data class CreateExpenseRequest(val category: String, val amount: Long, val memo: String? = null)
