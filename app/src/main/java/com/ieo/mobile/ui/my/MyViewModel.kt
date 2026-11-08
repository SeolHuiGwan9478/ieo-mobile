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

/** 내 정보 (M11): 이름 바꾸기, 로그아웃, 탈퇴 (ACC-003, ACC-004) */
class MyViewModel(private val accounts: AccountRepository) : ViewModel() {

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

    /** 성공하면 [onDone]을 부른다 (입력 창 닫기) */
    fun rename(name: String, onDone: () -> Unit) = run {
        when (val result = accounts.rename(name.trim())) {
            is ApiResult.Success -> {
                _state.update { it.copy(account = result.data) }
                onDone()
            }
            is ApiResult.Failure -> _state.update { it.copy(message = result.error.message) }
        }
    }

    /** 성공하면 세션이 끝나 시작 화면으로 돌아간다 */
    fun withdraw() = run {
        (accounts.withdraw() as? ApiResult.Failure)?.let { failure -> _state.update { it.copy(message = failure.error.message) } }
    }

    fun signOut() = accounts.signOut()

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
