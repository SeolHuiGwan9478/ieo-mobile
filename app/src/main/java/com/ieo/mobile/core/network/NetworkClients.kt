package com.ieo.mobile.core.network

import com.ieo.mobile.core.session.SessionManager
import com.ieo.mobile.core.session.TokenRefresher
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * 서버 연결에 쓰는 Retrofit 두 개를 만든다.
 * - [public]: 로그인 전 API (/api/auth/...). 토큰을 붙이지 않는다.
 * - [authenticated]: 나머지 API. 토큰을 붙이고, 만료되면 자동으로 갱신한다. 쓸 수 없는 계정이면 로그아웃한다.
 *
 * 토큰 갱신도 로그인 전 API라서, [refresherFactory]는 [public]을 받아 갱신기를 만든다.
 */
class NetworkClients(
    baseUrl: String,
    session: SessionManager,
    enableLogging: Boolean,
    refresherFactory: (Retrofit) -> TokenRefresher,
) {

    val json: Json = Json {
        // 서버가 필드를 늘려도 앱이 깨지지 않게
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .apply {
            if (enableLogging) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    val public: Retrofit = retrofit(baseUrl, baseClient)

    /** 로그인 후 클라이언트. Retrofit을 거치지 않는 연결(알림 SSE)이 쓴다 */
    val authenticatedClient: OkHttpClient = baseClient.newBuilder()
        .addInterceptor(BearerTokenInterceptor(session))
        .addInterceptor(RevokedSessionInterceptor(session))
        .authenticator(TokenRefreshAuthenticator(session, refresherFactory(public)))
        .build()

    val authenticated: Retrofit = retrofit(baseUrl, authenticatedClient)

    val caller = ApiCaller(json)

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
