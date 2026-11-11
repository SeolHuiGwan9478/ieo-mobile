package com.ieo.mobile.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.trip.CancelReason
import com.ieo.mobile.domain.trip.Trip
import com.ieo.mobile.domain.trip.TripRepository
import com.ieo.mobile.domain.trip.TripStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripsState(
    val trips: List<Trip> = emptyList(),
    val selectedId: Long? = null,
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val loadFailed: Boolean = false,
    val error: String? = null,
) {
    val selected: Trip? get() = selectedId?.let { id -> trips.firstOrNull { it.id == id } }

    val inProgress: List<Trip> get() = trips.filter { it.status.isInProgress }

    /** 상차가 가까운 순. 시각을 모르면 뒤로 */
    val upcoming: List<Trip>
        get() = trips.filter { it.status == TripStatus.SCHEDULED }
            .sortedWith(compareBy(nullsLast()) { it.pickup.plannedAt })

    val past: List<Trip> get() = trips.filter { it.status.isFinished }

    /** 홈에 보여 줄 다음 운행: 진행 중이 먼저 */
    val next: Trip? get() = inProgress.firstOrNull() ?: upcoming.firstOrNull()
}

/** 내 배차 (TRIP): 진행 중, 예정, 지난 운행을 본다. 운행 진행은 차량 앱이 맡는다 */
class TripsViewModel(private val repository: TripRepository) : ViewModel() {

    private val _state = MutableStateFlow(TripsState())
    val state: StateFlow<TripsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.list()) {
                is ApiResult.Success -> _state.update { it.copy(trips = result.data, isLoading = false, loadFailed = false) }
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, loadFailed = true, error = result.error.message) }
            }
        }
    }

    fun open(tripId: Long) = _state.update { it.copy(selectedId = tripId, error = null) }

    fun close() = _state.update { it.copy(selectedId = null, error = null) }

    /** 이에오 배차는 [reason]을 골라야 보낸다 */
    fun cancel(reason: CancelReason? = null) {
        val trip = _state.value.selected?.takeIf { it.canCancel } ?: return
        if (trip.needsCancelReason && reason == null) return
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.cancel(trip.id, reason)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(selectedId = null) }
                    refresh()
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            }
            _state.update { it.copy(isBusy = false) }
        }
    }
}
