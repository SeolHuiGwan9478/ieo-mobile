package com.ieo.mobile.core.session

/** 토큰을 기기에 보관하는 곳. 구현은 [EncryptedTokenStore] (테스트에서는 메모리 구현으로 바꿔 끼운다) */
interface TokenStore {

    fun read(): AuthTokens?

    fun write(tokens: AuthTokens)

    fun clear()
}
