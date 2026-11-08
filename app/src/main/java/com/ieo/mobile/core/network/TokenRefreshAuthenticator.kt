package com.ieo.mobile.core.network

import com.ieo.mobile.core.session.SessionManager
import com.ieo.mobile.core.session.TokenRefresher
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * 401을 받으면 리프레시 토큰으로 액세스 토큰을 새로 받아 같은 요청을 한 번 더 보낸다.
 * - 여러 요청이 동시에 401을 받아도 갱신은 한 번만 한다 (먼저 갱신한 토큰을 나머지가 같이 쓴다).
 * - 서버가 리프레시 토큰을 거절하면 로그아웃한다. 네트워크 문제라면 로그인은 그대로 둔다.
 */
class TokenRefreshAuthenticator(
    private val session: SessionManager,
    private val refresher: TokenRefresher,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 새 토큰으로 다시 보냈는데도 401이면 더 하지 않는다
        if (response.priorResponse != null) return null

        val sentToken = response.request.header(BearerTokenInterceptor.AUTHORIZATION)
        synchronized(lock) {
            val current = session.tokens() ?: return null
            val accessToken = if (sentToken != BearerTokenInterceptor.bearer(current.accessToken)) {
                // 기다리는 사이 다른 요청이 이미 갱신했다
                current.accessToken
            } else {
                when (val result = refresher.refresh(current.refreshToken)) {
                    is TokenRefresher.Result.Refreshed -> {
                        session.updateTokens(result.tokens)
                        result.tokens.accessToken
                    }
                    TokenRefresher.Result.Rejected -> {
                        session.signOut()
                        return null
                    }
                    TokenRefresher.Result.Unavailable -> return null
                }
            }
            return response.request.newBuilder()
                .header(BearerTokenInterceptor.AUTHORIZATION, BearerTokenInterceptor.bearer(accessToken))
                .build()
        }
    }
}
