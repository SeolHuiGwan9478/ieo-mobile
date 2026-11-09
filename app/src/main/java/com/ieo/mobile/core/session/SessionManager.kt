package com.ieo.mobile.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 로그인 상태의 단일 출처. 토큰 저장은 [TokenStore]에 맡기고, 로그인 여부를 [isLoggedIn]으로 알린다.
 * 화면은 [isLoggedIn]을 지켜보다가 false가 되면(로그아웃, 리프레시 토큰 만료) 로그인 화면으로 간다.
 */
class SessionManager(private val tokenStore: TokenStore) {

    // 매 요청마다 복호화하지 않도록 메모리에 들고 있는다
    @Volatile
    private var cached: AuthTokens? = tokenStore.read()

    private val _isLoggedIn = MutableStateFlow(cached != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun tokens(): AuthTokens? = cached

    /** 로그인·QR 승인 성공 */
    fun signIn(tokens: AuthTokens) {
        save(tokens)
        _isLoggedIn.value = true
    }

    /** 액세스 토큰 갱신. 로그인 상태는 그대로 */
    fun updateTokens(tokens: AuthTokens) = save(tokens)

    fun signOut() {
        cached = null
        tokenStore.clear()
        _isLoggedIn.value = false
    }

    private fun save(tokens: AuthTokens) {
        cached = tokens
        tokenStore.write(tokens)
    }
}
