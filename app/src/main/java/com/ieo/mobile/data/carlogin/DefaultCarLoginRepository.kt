package com.ieo.mobile.data.carlogin

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.carlogin.CarLoginRepository
import com.ieo.mobile.domain.carlogin.CarLoginRequest
import java.time.Instant
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CarLoginApi {

    @GET("api/car-login/requests/{code}")
    suspend fun find(@Path("code") code: String): Response<CarLoginRequestResponse>

    @POST("api/car-login/requests/{code}/approve")
    suspend fun approve(@Path("code") code: String): Response<Unit>

    @POST("api/car-login/requests/{code}/reject")
    suspend fun reject(@Path("code") code: String): Response<Unit>
}

@Serializable
data class CarLoginRequestResponse(
    val code: String,
    val deviceName: String? = null,
    val approxLocation: String? = null,
    val expiresAt: String,
    val status: String,
) {
    fun toDomain() = CarLoginRequest(
        code = code,
        deviceName = deviceName.orEmpty().ifBlank { "차량" },
        approxLocation = approxLocation?.takeIf { it.isNotBlank() },
        expiresAt = Instant.parse(expiresAt),
        isPending = status == "PENDING",
    )
}

class DefaultCarLoginRepository(
    private val api: CarLoginApi,
    private val caller: ApiCaller,
) : CarLoginRepository {

    override suspend fun find(code: String): ApiResult<CarLoginRequest> =
        caller.call { api.find(code) }.mapResponse { it.toDomain() }

    override suspend fun approve(code: String): ApiResult<Unit> = caller.callNoContent { api.approve(code) }

    override suspend fun reject(code: String): ApiResult<Unit> = caller.callNoContent { api.reject(code) }
}
