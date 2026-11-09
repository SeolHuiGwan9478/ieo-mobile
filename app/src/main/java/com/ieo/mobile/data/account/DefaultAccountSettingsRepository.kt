package com.ieo.mobile.data.account

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.account.AccountSettingsRepository
import com.ieo.mobile.domain.account.BusinessInfo
import com.ieo.mobile.domain.account.CarSession
import com.ieo.mobile.domain.account.ConsentItem
import java.time.Instant
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AccountSettingsApi {

    @GET("api/users/me/car-sessions")
    suspend fun carSessions(): Response<List<CarSessionResponse>>

    @POST("api/users/me/car-sessions/{id}/revoke")
    suspend fun revokeCarSession(@Path("id") id: String): Response<Unit>

    @GET("api/users/me/consents")
    suspend fun consents(): Response<List<ConsentResponse>>

    @PUT("api/users/me/consents/{type}")
    suspend fun changeConsent(@Path("type") type: String, @Body body: ChangeConsentRequest): Response<ConsentResponse>

    @GET("api/users/me/business")
    suspend fun business(): Response<BusinessInfoDto>

    @PUT("api/users/me/business")
    suspend fun saveBusiness(@Body body: BusinessInfoDto): Response<BusinessInfoDto>

    @DELETE("api/users/me/business")
    suspend fun clearBusiness(): Response<Unit>
}

@Serializable
data class CarSessionResponse(val id: String, val deviceName: String? = null, val lastUsedAt: String) {
    fun toDomain() = CarSession(id, deviceName, Instant.parse(lastUsedAt))
}

@Serializable
data class ConsentResponse(val type: String, val description: String, val required: Boolean, val agreed: Boolean) {
    fun toDomain() = ConsentItem(type, description, required, agreed)
}

@Serializable
data class ChangeConsentRequest(val agreed: Boolean)

@Serializable
data class BusinessInfoDto(
    val businessName: String? = null,
    val businessNumber: String? = null,
    val representativeName: String? = null,
) {
    fun toDomain(): BusinessInfo? =
        if (businessName == null || businessNumber == null || representativeName == null) null
        else BusinessInfo(businessName, businessNumber, representativeName)
}

class DefaultAccountSettingsRepository(
    private val api: AccountSettingsApi,
    private val caller: ApiCaller,
) : AccountSettingsRepository {

    override suspend fun carSessions(): ApiResult<List<CarSession>> =
        caller.call { api.carSessions() }.mapResponse { list -> list.map(CarSessionResponse::toDomain) }

    override suspend fun revokeCarSession(id: String): ApiResult<Unit> = caller.callNoContent { api.revokeCarSession(id) }

    override suspend fun consents(): ApiResult<List<ConsentItem>> =
        caller.call { api.consents() }.mapResponse { list -> list.map(ConsentResponse::toDomain) }

    override suspend fun changeConsent(type: String, agreed: Boolean): ApiResult<ConsentItem> =
        caller.call { api.changeConsent(type, ChangeConsentRequest(agreed)) }.mapResponse { it.toDomain() }

    override suspend fun business(): ApiResult<BusinessInfo?> = caller.call { api.business() }.mapResponse { it.toDomain() }

    override suspend fun saveBusiness(info: BusinessInfo): ApiResult<BusinessInfo?> =
        caller.call { api.saveBusiness(BusinessInfoDto(info.name, info.number, info.representative)) }.mapResponse { it.toDomain() }

    override suspend fun clearBusiness(): ApiResult<Unit> = caller.callNoContent { api.clearBusiness() }
}
