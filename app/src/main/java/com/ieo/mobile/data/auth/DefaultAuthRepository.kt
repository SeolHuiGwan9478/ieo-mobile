package com.ieo.mobile.data.auth

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.map
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.core.session.AuthTokens
import com.ieo.mobile.core.session.DeviceInfo
import com.ieo.mobile.core.session.SessionManager
import com.ieo.mobile.domain.auth.AuthRepository
import com.ieo.mobile.domain.auth.Consent
import com.ieo.mobile.domain.auth.PhoneVerification
import com.ieo.mobile.domain.vehicle.NewVehicle

class DefaultAuthRepository(
    private val api: AuthApi,
    private val caller: ApiCaller,
    private val session: SessionManager,
    private val device: DeviceInfo,
) : AuthRepository {

    override suspend fun requestCode(phone: String): ApiResult<String?> =
        caller.callOptional { api.requestCode(SendCodeRequest(phone)) }.map { it?.demoCode }

    override suspend fun verify(phone: String, code: String): ApiResult<PhoneVerification> =
        caller.call { api.verify(VerifyCodeRequest(phone, code, device.deviceId)) }.mapResponse { it.toDomain() }

    override suspend fun login(phoneToken: String): ApiResult<Unit> =
        caller.call { api.login(PhoneLoginRequest(phoneToken, device.deviceId)) }.map { session.signIn(it.toDomain()) }

    /** 가입했지만 완료 화면을 아직 닫지 않은 토큰 */
    private var pendingTokens: AuthTokens? = null

    override suspend fun signUp(phoneToken: String, name: String, consents: Set<Consent>, vehicle: NewVehicle?): ApiResult<Unit> {
        val body = SignUpRequest(
            phoneToken = phoneToken,
            deviceId = device.deviceId,
            role = DRIVER,
            name = name,
            consents = Consent.entries.map { ConsentRequest(it.name, CONSENT_VERSION, it in consents) },
            vehicle = vehicle?.let { SignUpVehicle(it.plateNumber, it.tonnage.name, it.bodyType.name) },
        )
        return caller.call { api.signUp(body) }.map { pendingTokens = it.toDomain() }
    }

    override fun finishSignUp() {
        pendingTokens?.let(session::signIn)
        pendingTokens = null
    }

    private companion object {
        const val DRIVER = "DRIVER"
        const val CONSENT_VERSION = "2026-11"
    }
}
