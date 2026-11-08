package com.ieo.mobile.data.link

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface DriverLinkApi {

    @GET("api/driver-links/invitations")
    suspend fun invitations(): Response<List<DriverLinkResponse>>

    @GET("api/driver-links")
    suspend fun links(): Response<List<DriverLinkResponse>>

    @POST("api/driver-links/invitations/{linkId}/accept")
    suspend fun accept(@Path("linkId") linkId: Long): Response<DriverLinkResponse>

    @POST("api/driver-links/invitations/{linkId}/decline")
    suspend fun decline(@Path("linkId") linkId: Long): Response<Unit>

    @POST("api/driver-links/{linkId}/unlink")
    suspend fun unlink(@Path("linkId") linkId: Long): Response<Unit>
}
