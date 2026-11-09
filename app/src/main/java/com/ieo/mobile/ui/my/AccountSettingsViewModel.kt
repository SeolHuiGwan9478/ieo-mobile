package com.ieo.mobile.ui.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.account.AccountSettingsRepository
import com.ieo.mobile.domain.account.BusinessInfo
import com.ieo.mobile.domain.account.CarSession
import com.ieo.mobile.domain.account.ConsentItem
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AccountSection { CARS, CONSENTS, BUSINESS }

data class BusinessForm(val name: String = "", val number: String = "", val representative: String = "") {
    val canSave get() = name.isNotBlank() && number.length == BUSINESS_NUMBER_DIGITS && representative.isNotBlank()
}

data class AccountSettingsState(
    val carSessions: List<CarSession> = emptyList(),
    val consents: List<ConsentItem> = emptyList(),
    val saved: BusinessInfo? = null,
    val form: BusinessForm = BusinessForm(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val loadFailed: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

private const val BUSINESS_NUMBER_DIGITS = 10

/** 로그인된 차량, 동의 관리, 사업자 정보 (ACC-003·005·006) */
class AccountSettingsViewModel(private val repository: AccountSettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow(AccountSettingsState())
    val state: StateFlow<AccountSettingsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, loadFailed = false, notice = null) }
            val cars = async { repository.carSessions() }
            val consents = async { repository.consents() }
            val business = async { repository.business() }
            apply(cars.await(), consents.await(), business.await())
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun revoke(session: CarSession) = change("${session.name ?: "차량"}에서 로그아웃했어요") {
        repository.revokeCarSession(session.id).also { result ->
            if (result is ApiResult.Success) _state.update { s -> s.copy(carSessions = s.carSessions - session) }
        }
    }

    /** 스위치가 바로 움직이도록 먼저 바꾸고, 실패하면 되돌린다 */
    fun setConsent(item: ConsentItem, agreed: Boolean) {
        if (item.required) return
        replaceConsent(item.copy(agreed = agreed))
        viewModelScope.launch {
            when (val result = repository.changeConsent(item.type, agreed)) {
                is ApiResult.Success -> replaceConsent(result.data)
                is ApiResult.Failure -> {
                    replaceConsent(item)
                    _state.update { it.copy(error = result.error.message) }
                }
            }
        }
    }

    fun onBusinessName(value: String) = editForm { it.copy(name = value.take(50)) }

    fun onBusinessNumber(value: String) = editForm { it.copy(number = value.filter(Char::isDigit).take(BUSINESS_NUMBER_DIGITS)) }

    fun onRepresentative(value: String) = editForm { it.copy(representative = value.take(30)) }

    fun saveBusiness() {
        val form = _state.value.form
        if (!form.canSave) return
        change("저장했어요") {
            when (val result = repository.saveBusiness(BusinessInfo(form.name.trim(), form.number, form.representative.trim()))) {
                is ApiResult.Success -> {
                    _state.update { it.copy(saved = result.data) }
                    ApiResult.Success(Unit)
                }
                is ApiResult.Failure -> result
            }
        }
    }

    fun clearBusiness() = change("사업자 정보를 지웠어요") {
        repository.clearBusiness().also { result ->
            if (result is ApiResult.Success) _state.update { it.copy(saved = null, form = BusinessForm()) }
        }
    }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    private fun apply(
        cars: ApiResult<List<CarSession>>,
        consents: ApiResult<List<ConsentItem>>,
        business: ApiResult<BusinessInfo?>,
    ) {
        (cars as? ApiResult.Success)?.let { ok -> _state.update { it.copy(carSessions = ok.data) } }
        (consents as? ApiResult.Success)?.let { ok -> _state.update { it.copy(consents = ok.data) } }
        (business as? ApiResult.Success)?.let { ok ->
            _state.update { it.copy(saved = ok.data, form = ok.data?.let { b -> BusinessForm(b.name, b.number, b.representative) } ?: it.form) }
        }
        listOf(cars, consents, business).filterIsInstance<ApiResult.Failure>().firstOrNull()?.let { failure ->
            _state.update { it.copy(loadFailed = true, error = failure.error.message) }
        }
    }

    private fun replaceConsent(item: ConsentItem) = _state.update { s ->
        s.copy(consents = s.consents.map { if (it.type == item.type) item else it })
    }

    private fun editForm(block: (BusinessForm) -> BusinessForm) = _state.update { it.copy(form = block(it.form), error = null, notice = null) }

    private fun change(notice: String, block: suspend () -> ApiResult<Unit>) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                when (val result = block()) {
                    is ApiResult.Success -> _state.update { it.copy(notice = notice) }
                    is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
                }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }
}
