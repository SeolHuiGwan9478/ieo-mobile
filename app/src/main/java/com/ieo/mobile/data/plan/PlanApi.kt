package com.ieo.mobile.data.plan

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PlanApi {

    @GET("api/plans/me")
    suspend fun mine(): Response<DriverPlanResponse>

    @PUT("api/plans/me")
    suspend fun choose(@Body body: ChoosePlanRequest): Response<DriverPlanResponse>

    @POST("api/plans/me/trial")
    suspend fun startTrial(): Response<DriverPlanResponse>

    @POST("api/plans/me/resume")
    suspend fun resume(): Response<DriverPlanResponse>

    @POST("api/plans/me/coupons/{couponId}/use")
    suspend fun useCoupon(@Path("couponId") couponId: Long): Response<DriverPlanResponse>
}
