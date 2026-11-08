package com.ieo.mobile.domain.account

import com.ieo.mobile.core.network.ApiResult

data class Account(val name: String, val phone: String)

interface AccountRepository {

    suspend fun me(): ApiResult<Account>

    suspend fun rename(name: String): ApiResult<Account>

    /** 탈퇴. 되돌릴 수 없다. 성공하면 로그아웃된다 */
    suspend fun withdraw(): ApiResult<Unit>

    fun signOut()
}
