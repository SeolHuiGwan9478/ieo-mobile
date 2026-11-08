package com.ieo.mobile.data.auth

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** 로그인 전 API (/api/auth/...) */
interface AuthApi {

    @POST("api/auth/phone/code")
    suspend fun requestCode(@Body body: SendCodeRequest): Response<SendCodeResponse>

    @POST("api/auth/phone/verify")
    suspend fun verify(@Body body: VerifyCodeRequest): Response<PhoneVerifyResponse>

    @POST("api/auth/phone/login")
    suspend fun login(@Body body: PhoneLoginRequest): Response<TokenResponse>

    @POST("api/auth/signup")
    suspend fun signUp(@Body body: SignUpRequest): Response<TokenResponse>

    /** 토큰 갱신. OkHttp Authenticator 안에서 부르므로 동기 호출(Call)이다 */
    @POST("api/auth/refresh")
    fun refresh(@Body body: RefreshTokenRequest): Call<TokenResponse>
}
