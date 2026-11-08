package com.ieo.mobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.auth.AuthRepository
import com.ieo.mobile.domain.auth.Consent
import com.ieo.mobile.domain.auth.ExistingAccount
import com.ieo.mobile.domain.vehicle.BodyType
import com.ieo.mobile.domain.vehicle.NewVehicle
import com.ieo.mobile.domain.vehicle.Tonnage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginStep { START, PHONE, CODE, TERMS, NAME, VEHICLE, DONE }

data class LoginState(
    val step: LoginStep = LoginStep.START,
    val phone: String = "",
    val code: String = "",
    val name: String = "",
    val plateNumber: String = "",
    val tonnage: Tonnage = Tonnage.TON_5,
    val bodyType: BodyType = BodyType.CARGO,
    val consents: Set<Consent> = emptySet(),
    /** 이 번호로 가입한 계정이 있으면 확인 창을 띄운다 */
    val existing: ExistingAccount? = null,
    /** 인증번호가 틀렸을 때 칸을 빨갛게 한다 */
    val codeRejected: Boolean = false,
    /** 인증번호 유효 시간이 지났다 */
    val codeExpired: Boolean = false,
    val codeExpiresAtMillis: Long = 0,
    val resendAtMillis: Long = 0,
    /** 로컬 서버가 준 시연용 인증번호 */
    val demoCode: String? = null,
    val isBusy: Boolean = false,
    val error: String? = null,
) {
    val canRequestCode get() = phone.matches(PHONE_PATTERN) && !isBusy
    val canVerify get() = code.length == CODE_LENGTH && !isBusy && !codeExpired
    val canAgree get() = Consent.entries.filter { it.required }.all { it in consents }
    val allAgreed get() = consents.size == Consent.entries.size
    val canSignUp get() = name.isNotBlank() && !isBusy

    companion object {
        const val CODE_LENGTH = 6
        val PHONE_PATTERN = Regex("^01\\d{8,9}$")
    }
}

/**
 * 시작 → 번호 → 인증번호 → (가입된 번호면 확인 창) → 약관 → 기본 정보 → 완료 (ACC-001, ACC-002).
 * 이 앱은 차주용이라 주선사 계정은 들어오지 못하게 안내한다.
 */
class LoginViewModel(
    private val auth: AuthRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private var phoneToken: String? = null

    fun start() = _state.update { it.copy(step = LoginStep.PHONE, error = null) }

    fun onPhoneChange(value: String) = _state.update { it.copy(phone = value.filter(Char::isDigit).take(11), error = null) }

    /** 6자리를 다 넣으면 바로 확인한다 */
    fun onCodeChange(value: String) {
        _state.update { it.copy(code = value.filter(Char::isDigit).take(LoginState.CODE_LENGTH), error = null, codeRejected = false) }
        if (_state.value.canVerify) verify()
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value.take(30), error = null) }

    fun onPlateChange(value: String) = _state.update { it.copy(plateNumber = value.take(20), error = null) }

    fun onTonnage(value: Tonnage) = _state.update { it.copy(tonnage = value) }

    fun onBodyType(value: BodyType) = _state.update { it.copy(bodyType = value) }

    fun toggleConsent(consent: Consent) = _state.update {
        it.copy(consents = if (consent in it.consents) it.consents - consent else it.consents + consent)
    }

    fun toggleAllConsents() = _state.update {
        it.copy(consents = if (it.allAgreed) emptySet() else Consent.entries.toSet())
    }

    /** 처음 받기와 다시 받기 모두 이걸로 한다 */
    fun requestCode() = busy {
        when (val result = auth.requestCode(_state.value.phone)) {
            is ApiResult.Success -> _state.update {
                val sentAt = now()
                it.copy(
                    step = LoginStep.CODE,
                    code = "",
                    demoCode = result.data,
                    codeRejected = false,
                    codeExpired = false,
                    codeExpiresAtMillis = sentAt + CODE_TTL_MILLIS,
                    resendAtMillis = sentAt + RESEND_MILLIS,
                )
            }
            is ApiResult.Failure -> fail(result.error.message)
        }
    }

    fun verify() = busy {
        val current = _state.value
        when (val result = auth.verify(current.phone, current.code)) {
            is ApiResult.Success -> {
                phoneToken = result.data.phoneToken
                val existing = result.data.existingAccount
                _state.update {
                    when {
                        existing == null -> it.copy(step = LoginStep.TERMS)
                        existing.isDriver -> it.copy(existing = existing)
                        else -> it.copy(step = LoginStep.PHONE, code = "", error = BROKER_MESSAGE)
                    }
                }
            }
            is ApiResult.Failure -> onCodeRejected(result.error)
        }
    }

    /** 화면의 남은 시간이 0이 되면 부른다 */
    fun onCodeTimeUp() = _state.update { it.copy(codeExpired = true) }

    fun login() = busy {
        val token = phoneToken ?: return@busy fail(EXPIRED_MESSAGE)
        (auth.login(token) as? ApiResult.Failure)?.let { fail(it.error.message) }
    }

    fun agreeToTerms() = _state.update { it.copy(step = LoginStep.NAME, error = null) }

    fun nameDone() = _state.update { it.copy(step = LoginStep.VEHICLE, error = null) }

    /** [withVehicle]가 false면 "나중에"를 누른 것 */
    fun signUp(withVehicle: Boolean = true) = busy {
        val token = phoneToken ?: return@busy fail(EXPIRED_MESSAGE)
        val current = _state.value
        val vehicle = current.plateNumber.trim().takeIf { it.isNotEmpty() && withVehicle }
            ?.let { NewVehicle(it, current.tonnage, current.bodyType) }
        when (val result = auth.signUp(token, current.name.trim(), current.consents, vehicle)) {
            is ApiResult.Success -> _state.update { it.copy(step = LoginStep.DONE) }
            is ApiResult.Failure -> fail(result.error.message)
        }
    }

    /** 가입 완료 화면의 "시작하기" */
    fun finish() = auth.finishSignUp()

    /** 이미 가입된 계정 창의 "처음으로" */
    fun restart() {
        phoneToken = null
        _state.value = LoginState()
    }

    /** 뒤로 갈 곳이 있으면 true */
    fun back(): Boolean {
        val previous = when (_state.value.step) {
            LoginStep.PHONE -> LoginStep.START
            LoginStep.CODE -> LoginStep.PHONE
            LoginStep.TERMS -> LoginStep.CODE
            LoginStep.NAME -> LoginStep.TERMS
            LoginStep.VEHICLE -> LoginStep.NAME
            LoginStep.START, LoginStep.DONE -> return false
        }
        _state.update { it.copy(step = previous, error = null, codeRejected = false) }
        return true
    }

    private fun onCodeRejected(error: ApiError) {
        val code = (error as? ApiError.Server)?.code
        _state.update {
            it.copy(
                error = error.message,
                codeRejected = code == WRONG_CODE,
                codeExpired = it.codeExpired || code == EXPIRED_CODE,
            )
        }
    }

    private fun fail(message: String) = _state.update { it.copy(error = message) }

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
        const val BROKER_MESSAGE = "주선사 계정이에요. 주선사 콘솔에서 로그인해 주세요."
        const val EXPIRED_MESSAGE = "인증이 만료됐어요. 처음부터 다시 해 주세요."
        const val CODE_TTL_MILLIS = 180_000L
        const val RESEND_MILLIS = 60_000L
        private const val WRONG_CODE = "AUTH_400_CODE"
        private const val EXPIRED_CODE = "AUTH_410_CODE"
    }
}
