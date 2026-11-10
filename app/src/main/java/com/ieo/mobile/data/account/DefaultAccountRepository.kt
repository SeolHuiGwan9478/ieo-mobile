package com.ieo.mobile.data.account

import com.ieo.mobile.core.session.DeviceInfo
import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.map
import com.ieo.mobile.core.session.SessionManager
import com.ieo.mobile.domain.account.Account
import com.ieo.mobile.domain.account.AccountRepository
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AccountApi {

    @GET("api/users/me")
    suspend fun me(): Response<UserResponse>

    @PATCH("api/users/me")
    suspend fun rename(@Body body: UpdateUserRequest): Response<UserResponse>

    @POST("api/users/me/phone")
    suspend fun changePhone(@Body body: ChangePhoneRequest): Response<UserResponse>

    @POST("api/users/me/withdraw")
    suspend fun withdraw(): Response<Unit>
}

@Serializable
data class UpdateUserRequest(val name: String)

@Serializable
data class ChangePhoneRequest(val phoneToken: String, val deviceId: String)

@Serializable
data class UserResponse(val name: String? = null, val phone: String? = null)

class DefaultAccountRepository(
    private val api: AccountApi,
    private val caller: ApiCaller,
    private val session: SessionManager,
    private val device: DeviceInfo,
) : AccountRepository {

    override suspend fun me(): ApiResult<Account> =
        caller.call { api.me() }.map { Account(it.name.orEmpty(), it.phone.orEmpty()) }

    override suspend fun rename(name: String): ApiResult<Account> =
        caller.call { api.rename(UpdateUserRequest(name)) }.map { Account(it.name.orEmpty(), it.phone.orEmpty()) }

    override suspend fun changePhone(phoneToken: String): ApiResult<Account> =
        caller.call { api.changePhone(ChangePhoneRequest(phoneToken, device.deviceId)) }.map { Account(it.name.orEmpty(), it.phone.orEmpty()) }

    override suspend fun withdraw(): ApiResult<Unit> =
        caller.callNoContent { api.withdraw() }.map { session.signOut() }

    override fun signOut() = session.signOut()
}
