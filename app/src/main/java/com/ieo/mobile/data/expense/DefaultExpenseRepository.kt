package com.ieo.mobile.data.expense

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.expense.Expense
import com.ieo.mobile.domain.expense.ExpenseCategory
import com.ieo.mobile.domain.expense.ExpenseRepository
import java.time.LocalDate

class DefaultExpenseRepository(
    private val api: ExpenseApi,
    private val caller: ApiCaller,
) : ExpenseRepository {

    override suspend fun expenses(from: LocalDate, to: LocalDate): ApiResult<List<Expense>> =
        caller.call { api.expenses(from.toString(), to.toString()) }.mapResponse { list -> list.map(ExpenseResponse::toDomain) }

    override suspend fun add(category: ExpenseCategory, amount: Long, memo: String?): ApiResult<Expense> =
        caller.call { api.add(CreateExpenseRequest(category.name, amount, memo?.takeIf { it.isNotBlank() })) }
            .mapResponse { it.toDomain() }
}
