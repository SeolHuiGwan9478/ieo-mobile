package com.ieo.mobile.data.expense

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.Query

interface ExpenseApi {

    @GET("api/expenses")
    suspend fun expenses(@Query("from") from: String, @Query("to") to: String): Response<List<ExpenseResponse>>

    @POST("api/expenses")
    suspend fun add(@Body body: CreateExpenseRequest): Response<ExpenseResponse>

    @DELETE("api/expenses/{id}")
    suspend fun delete(@Path("id") id: Long): Response<Unit>
}
