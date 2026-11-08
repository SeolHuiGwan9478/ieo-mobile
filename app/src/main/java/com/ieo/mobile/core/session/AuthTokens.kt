package com.ieo.mobile.core.session

/** 서버가 로그인 때 주는 토큰 한 쌍. 액세스 30분, 리프레시 30일 */
data class AuthTokens(val accessToken: String, val refreshToken: String)
