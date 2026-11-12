package com.ieo.mobile.data.ledger

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LedgerApi {

    /** 날짜를 비우면 서버의 오늘(한국 날짜) */
    @GET("api/ledger/daily")
    suspend fun daily(): Response<DailySummaryResponse>

    /** CSV 파일 (LEDG-033) */
    @GET("api/ledger/export")
    suspend fun export(@Query("from") from: String, @Query("to") to: String): Response<ResponseBody>

    @GET("api/ledger/receivables")
    suspend fun receivables(): Response<ReceivablesResponse>

    /** 날짜는 2026-11-03 형식, 둘 다 포함 */
    @GET("api/ledger")
    suspend fun entries(@Query("from") from: String, @Query("to") to: String): Response<List<LedgerEntryResponse>>

    @POST("api/ledger")
    suspend fun addManual(@Body body: CreateLedgerEntryRequest): Response<LedgerEntryResponse>

    @PATCH("api/ledger/{entryId}")
    suspend fun update(@Path("entryId") entryId: Long, @Body body: UpdateLedgerEntryRequest): Response<LedgerEntryResponse>

    @POST("api/ledger/{entryId}/paid")
    suspend fun markPaid(@Path("entryId") entryId: Long): Response<LedgerEntryResponse>

    @GET("api/ledger/{entryId}/tax-invoice")
    suspend fun taxInvoice(@Path("entryId") entryId: Long): Response<TaxInvoiceResponse>

    @POST("api/ledger/{entryId}/tax-invoice")
    suspend fun issueTaxInvoice(@Path("entryId") entryId: Long): Response<TaxInvoiceResponse>

    @DELETE("api/ledger/{entryId}")
    suspend fun delete(@Path("entryId") entryId: Long): Response<Unit>

    @DELETE("api/ledger/{entryId}/paid")
    suspend fun unmarkPaid(@Path("entryId") entryId: Long): Response<LedgerEntryResponse>

    @POST("api/ledger/extra-incomes/{id}/paid")
    suspend fun markExtraIncomePaid(@Path("id") extraIncomeId: Long): Response<Unit>

    @DELETE("api/ledger/extra-incomes/{id}/paid")
    suspend fun unmarkExtraIncomePaid(@Path("id") extraIncomeId: Long): Response<Unit>
}
