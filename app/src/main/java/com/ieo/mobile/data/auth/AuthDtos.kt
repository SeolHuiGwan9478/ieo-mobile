package com.ieo.mobile.data.auth

import com.ieo.mobile.core.session.AuthTokens
import com.ieo.mobile.domain.auth.ExistingAccount
import com.ieo.mobile.domain.auth.PhoneVerification
import java.time.Instant
import kotlinx.serialization.Serializable

// 서버 domain/user/dto와 같은 모양

@Serializable
data class SendCodeRequest(val phone: String)

/** 로컬 서버에서만 온다. 다른 환경은 본문 없이 204 */
@Serializable
data class SendCodeResponse(val demoCode: String? = null)

@Serializable
data class VerifyCodeRequest(val phone: String, val code: String, val deviceId: String)

@Serializable
data class PhoneVerifyResponse(
    val phoneToken: String,
    val accountExists: Boolean,
    val existingAccount: ExistingAccountResponse? = null,
) {
    fun toDomain() = PhoneVerification(phoneToken, existingAccount?.takeIf { accountExists }?.toDomain())
}

@Serializable
data class ExistingAccountResponse(
    val name: String? = null,
    val role: String,
    val joinedAt: String? = null,
    val lastLoginAt: String? = null,
) {
    fun toDomain() = ExistingAccount(
        name = name.orEmpty(),
        isDriver = role == "DRIVER",
        joinedAt = joinedAt?.let(Instant::parse),
        lastLoginAt = lastLoginAt?.let(Instant::parse),
    )
}

@Serializable
data class PhoneLoginRequest(val phoneToken: String, val deviceId: String)

@Serializable
data class ConsentRequest(val type: String, val version: String, val agreed: Boolean)

@Serializable
data class SignUpRequest(
    val phoneToken: String,
    val deviceId: String,
    val role: String,
    val name: String,
    val consents: List<ConsentRequest>,
    val vehicle: SignUpVehicle? = null,
)

@Serializable
data class SignUpVehicle(val plateNumber: String, val tonnage: String, val bodyType: String)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class TokenResponse(val accessToken: String, val refreshToken: String) {
    fun toDomain() = AuthTokens(accessToken, refreshToken)
}
