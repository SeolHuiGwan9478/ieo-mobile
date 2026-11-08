package com.ieo.mobile.data.auth

import com.ieo.mobile.core.session.TokenRefresher
import java.io.IOException

/** POST /api/auth/refresh로 토큰을 새로 받는다 */
class ServerTokenRefresher(private val api: AuthApi) : TokenRefresher {

    override fun refresh(refreshToken: String): TokenRefresher.Result = try {
        val response = api.refresh(RefreshTokenRequest(refreshToken)).execute()
        val body = response.body()
        when {
            response.isSuccessful && body != null -> TokenRefresher.Result.Refreshed(body.toDomain())
            // 4xx: 서버가 이 리프레시 토큰을 받아 주지 않음
            response.code() in 400..499 -> TokenRefresher.Result.Rejected
            else -> TokenRefresher.Result.Unavailable
        }
    } catch (e: IOException) {
        TokenRefresher.Result.Unavailable
    }
}
