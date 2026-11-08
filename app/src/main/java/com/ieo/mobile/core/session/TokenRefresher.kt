package com.ieo.mobile.core.session

/** 리프레시 토큰으로 새 토큰을 받는다. OkHttp 스레드에서 불리므로 동기 함수다 */
fun interface TokenRefresher {

    fun refresh(refreshToken: String): Result

    sealed interface Result {
        data class Refreshed(val tokens: AuthTokens) : Result

        /** 서버가 거절함 (만료, 위조). 다시 로그인해야 한다 */
        data object Rejected : Result

        /** 서버에 닿지 못함. 로그인은 유지한다 */
        data object Unavailable : Result
    }
}
