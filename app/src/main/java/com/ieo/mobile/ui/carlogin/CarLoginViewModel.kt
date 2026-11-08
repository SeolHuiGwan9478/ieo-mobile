package com.ieo.mobile.ui.carlogin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.carlogin.CarLoginRepository
import com.ieo.mobile.domain.carlogin.CarLoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CarLoginState {
    data object Loading : CarLoginState
    data class Asking(val request: CarLoginRequest, val isBusy: Boolean = false, val error: String? = null) : CarLoginState
    data object Approved : CarLoginState
    data object Rejected : CarLoginState
    data class Failed(val message: String) : CarLoginState
}

/** 차량 화면의 QR을 찍은 뒤 승인한다. 코드가 내 차 화면과 같은지 사람이 확인한다 */
class CarLoginViewModel(private val repository: CarLoginRepository) : ViewModel() {

    private val _state = MutableStateFlow<CarLoginState>(CarLoginState.Loading)
    val state: StateFlow<CarLoginState> = _state.asStateFlow()

    fun load(code: String) {
        _state.value = CarLoginState.Loading
        viewModelScope.launch {
            _state.value = when (val result = repository.find(code)) {
                is ApiResult.Success ->
                    if (result.data.isPending) CarLoginState.Asking(result.data) else CarLoginState.Failed(NOT_PENDING_MESSAGE)
                is ApiResult.Failure -> CarLoginState.Failed(result.error.message)
            }
        }
    }

    fun approve() = answer(CarLoginState.Approved, repository::approve)

    fun reject() = answer(CarLoginState.Rejected, repository::reject)

    private fun answer(done: CarLoginState, send: suspend (String) -> ApiResult<Unit>) {
        val asking = _state.value as? CarLoginState.Asking ?: return
        if (asking.isBusy) return
        _state.value = asking.copy(isBusy = true, error = null)
        viewModelScope.launch {
            _state.value = when (val result = send(asking.request.code)) {
                is ApiResult.Success -> done
                is ApiResult.Failure -> asking.copy(isBusy = false, error = result.error.message)
            }
        }
    }

    companion object {
        const val NOT_PENDING_MESSAGE = "이미 처리됐거나 시간이 지난 요청이에요. 차량에서 새 QR을 받아 주세요."
    }
}
