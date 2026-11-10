package com.ieo.mobile.data.order

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OrderApi {

    /** 위치를 보내면 반경(기본 50km) 안 상차지만 준다 */
    @GET("api/orders")
    suspend fun openOrders(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("size") size: Int? = null,
        @Query("radiusM") radiusM: Int? = null,
        @Query("minFare") minFare: Long? = null,
        @Query("paymentType") paymentType: String? = null,
    ): Response<List<OrderResponse>>

    /** 지금 위치에서 이 오더를 맡을 때의 실수익 내역 */
    @GET("api/orders/{orderId}/profit")
    suspend fun profit(
        @Path("orderId") orderId: Long,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
    ): Response<ProfitEstimateResponse>

    /** 차량을 비우면 기본 차량 */
    @POST("api/orders/{orderId}/requests")
    suspend fun request(
        @Path("orderId") orderId: Long,
        @Body body: CreateDispatchRequestRequest = CreateDispatchRequestRequest(),
    ): Response<DispatchRequestResponse>

    @POST("api/dispatch-requests/{requestId}/withdraw")
    suspend fun withdraw(@Path("requestId") requestId: Long): Response<DispatchRequestResponse>

    @POST("api/orders/{orderId}/reports")
    suspend fun report(@Path("orderId") orderId: Long, @Body body: CreateOrderReportRequest): Response<Unit>
}
