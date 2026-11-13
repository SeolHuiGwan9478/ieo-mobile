package com.ieo.mobile.ui.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.account.Account
import com.ieo.mobile.domain.account.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyState(
    val account: Account? = null,
    val isBusy: Boolean = false,
    val message: String? = null,
)

/** 내 정보 (M11): 계정 값, 로그아웃, 탈퇴 (ACC-003, ACC-004). 이름·번호 고치기는 ProfileEditViewModel */
class MyViewModel(
    private val accounts: AccountRepository,
    /** 로그아웃 전에 이 휴대폰의 푸시 토큰을 서버에서 지운다 (로그인 토큰이 있어야 한다) */
    private val beforeSignOut: suspend () -> Unit = {},
) : ViewModel() {

    private val _state = MutableStateFlow(MyState())
    val state: StateFlow<MyState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = accounts.me()) {
                is ApiResult.Success -> _state.update { it.copy(account = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(message = result.error.message) }
            }
        }
    }

    /** 내 정보 수정에서 저장한 값으로 바꾼다 */
    fun updated(account: Account) = _state.update { it.copy(account = account, message = null) }

    /** 성공하면 세션이 끝나 시작 화면으로 돌아간다 */
    fun withdraw() = run {
        (accounts.withdraw() as? ApiResult.Failure)?.let { failure -> _state.update { it.copy(message = failure.error.message) } }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { beforeSignOut() }
            accounts.signOut()
        }
    }

    private fun run(block: suspend () -> Unit) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }
}
