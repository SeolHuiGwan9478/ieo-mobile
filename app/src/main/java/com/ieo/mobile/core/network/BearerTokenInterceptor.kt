package com.ieo.mobile.core.network

import com.ieo.mobile.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/** 로그인이 필요한 요청에 `Authorization: Bearer {accessToken}`을 붙인다 */
class BearerTokenInterceptor(private val session: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = session.tokens()?.accessToken ?: return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .header(AUTHORIZATION, bearer(accessToken))
            .build()
        return chain.proceed(request)
    }

    companion object {
        const val AUTHORIZATION = "Authorization"

        fun bearer(accessToken: String) = "Bearer $accessToken"
    }
}
