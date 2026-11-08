package com.ieo.mobile.core.network

import com.ieo.mobile.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 토큰은 유효한데 이 계정으로 차량 앱을 쓸 수 없다고 서버가 답하면 로그아웃한다.
 * 그대로 두면 모든 요청이 거절되어 빈 홈만 보인다 (에뮬레이터 확인: DB를 새로 만든 뒤 남은 예전 토큰).
 *
 * - `USER_403`: 이용 제한·탈퇴한 계정
 * - `..._403_ROLE`: 차주 프로필이 없는 계정 (차량 앱은 차주만 쓴다)
 *
 * 그 사이 다른 계정으로 다시 로그인했으면 새 세션은 건드리지 않는다.
 */
class RevokedSessionInterceptor(private val session: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (isRevoked(response) && sentCurrentToken(response)) session.signOut()
        return response
    }

    private fun sentCurrentToken(response: Response): Boolean {
        val current = session.tokens()?.accessToken ?: return false
        return response.request.header(BearerTokenInterceptor.AUTHORIZATION) == BearerTokenInterceptor.bearer(current)
    }

    companion object {
        private const val FORBIDDEN = 403
        private const val PEEK_BYTES = 4_096L
        private val REVOKED_CODE = Regex("\"code\"\\s*:\\s*\"(USER_403|[A-Z]+_403_ROLE)\"")

        fun isRevoked(response: Response): Boolean {
            if (response.code != FORBIDDEN) return false
            val body = runCatching { response.peekBody(PEEK_BYTES).string() }.getOrDefault("")
            return REVOKED_CODE.containsMatchIn(body)
        }
    }
}
