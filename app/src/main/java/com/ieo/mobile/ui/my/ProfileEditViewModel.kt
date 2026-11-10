package com.ieo.mobile.ui.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.account.Account
import com.ieo.mobile.domain.account.AccountRepository
import com.ieo.mobile.domain.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileEditState(
    val original: Account? = null,
    val name: String = "",
    /** 숫자만 */
    val phone: String = "",
    val codeSent: Boolean = false,
    /** 시연 서버가 돌려준 인증번호. 운영에서는 문자로만 간다 */
    val demoCode: String? = null,
    val code: String = "",
    /** 새 번호를 인증하고 받은 토큰. 있으면 번호를 바꿀 수 있다 */
    val phoneToken: String? = null,
    val isBusy: Boolean = false,
    val error: String? = null,
) {
    val nameChanged: Boolean get() = original != null && name.isNotBlank() && name.trim() != original.name
    val phoneChanged: Boolean get() = original != null && phone != original.phone
    val phoneVerified: Boolean get() = phoneToken != null
    val canSendCode: Boolean get() = phoneChanged && PHONE_PATTERN.matches(phone) && !isBusy
    val canVerify: Boolean get() = codeSent && code.length == CODE_LENGTH && !phoneVerified && !isBusy
    val canSave: Boolean get() = !isBusy && name.isNotBlank() && (nameChanged || phoneChanged) && (!phoneChanged || phoneVerified)

    companion object {
        val PHONE_PATTERN = Regex("^01\\d{8,9}$")
        const val CODE_LENGTH = 6
    }
}

/**
 * 내 정보 수정 (ACC-003, ACC-008): 이름과 휴대폰 번호. 번호를 바꾸려면 새 번호로 인증번호를 받아 확인한다.
 * 다른 계정이 쓰는 번호면 확인 단계에서 막는다
 */
class ProfileEditViewModel(
    private val accounts: AccountRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileEditState())
    val state: StateFlow<ProfileEditState> = _state.asStateFlow()

    /** 지금 계정 값으로 채운다 */
    fun start(account: Account) {
        _state.value = ProfileEditState(original = account, name = account.name, phone = account.phone)
    }

    fun onName(value: String) = _state.update { it.copy(name = value.take(30), error = null) }

    /** 번호를 고치면 인증을 처음부터 다시 한다 */
    fun onPhone(value: String) = _state.update {
        it.copy(phone = value.filter(Char::isDigit).take(11), codeSent = false, demoCode = null, code = "", phoneToken = null, error = null)
    }

    fun onCode(value: String) = _state.update { it.copy(code = value.filter(Char::isDigit).take(ProfileEditState.CODE_LENGTH), error = null) }

    fun sendCode() = busy {
        when (val result = auth.requestCode(_state.value.phone)) {
            is ApiResult.Success -> _state.update { it.copy(codeSent = true, demoCode = result.data, code = "") }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
        }
    }

    fun verify() = busy {
        val current = _state.value
        when (val result = auth.verify(current.phone, current.code)) {
            is ApiResult.Success ->
                if (result.data.existingAccount != null) {
                    _state.update { it.copy(error = TAKEN_MESSAGE) }
                } else {
                    _state.update { it.copy(phoneToken = result.data.phoneToken) }
                }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
        }
    }

    /** 바뀐 것만 보낸다. 다 되면 [onDone]에 새 계정 값을 넘긴다 */
    fun save(onDone: (Account) -> Unit) = busy {
        val current = _state.value
        var account = current.original ?: return@busy
        if (current.nameChanged) {
            when (val result = accounts.rename(current.name.trim())) {
                is ApiResult.Success -> account = result.data
                is ApiResult.Failure -> return@busy _state.update { it.copy(error = result.error.message) }
            }
        }
        val token = current.phoneToken
        if (current.phoneChanged && token != null) {
            when (val result = accounts.changePhone(token)) {
                is ApiResult.Success -> account = result.data
                is ApiResult.Failure -> return@busy _state.update { it.copy(error = result.error.message, phoneToken = null, codeSent = false) }
            }
        }
        onDone(account)
    }

    private fun busy(block: suspend () -> Unit) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    companion object {
        const val TAKEN_MESSAGE = "다른 계정이 쓰는 번호예요"
    }
}
