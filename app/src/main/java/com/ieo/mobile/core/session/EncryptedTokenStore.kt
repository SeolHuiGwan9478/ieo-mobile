package com.ieo.mobile.core.session

import android.content.Context

/** 토큰을 [TokenCipher]로 암호화해 SharedPreferences에 저장한다 */
class EncryptedTokenStore(
    context: Context,
    private val cipher: TokenCipher,
) : TokenStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): AuthTokens? {
        val access = prefs.getString(KEY_ACCESS_TOKEN, null)?.let(cipher::decrypt)
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null)?.let(cipher::decrypt)
        if (access == null || refresh == null) {
            // 키가 사라졌거나(앱 데이터 복원 등) 값이 망가졌으면 다시 로그인하게 한다
            if (prefs.all.isNotEmpty()) clear()
            return null
        }
        return AuthTokens(access, refresh)
    }

    override fun write(tokens: AuthTokens) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, cipher.encrypt(tokens.accessToken))
            .putString(KEY_REFRESH_TOKEN, cipher.encrypt(tokens.refreshToken))
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "ieo_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
