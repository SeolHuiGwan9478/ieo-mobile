package com.ieo.mobile.ui.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.order.CargoCategory
import com.ieo.mobile.domain.recommendation.RecommendationPreference
import com.ieo.mobile.domain.recommendation.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecommendationState(
    /** 서버에 저장된 값 */
    val saved: RecommendationPreference? = null,
    /** 화면에서 고치는 값 */
    val draft: RecommendationPreference = RecommendationPreference(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val loadFailed: Boolean = false,
    val error: String? = null,
    val justSaved: Boolean = false,
) {
    val hasChanges: Boolean get() = saved != null && draft != saved
}

/** 다음 배차 추천 설정 (REC-004, 025) */
class RecommendationViewModel(private val repository: RecommendationRepository) : ViewModel() {

    private val _state = MutableStateFlow(RecommendationState())
    val state: StateFlow<RecommendationState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadFailed = false, error = null) }
            when (val result = repository.preference()) {
                is ApiResult.Success -> _state.update { it.copy(saved = result.data, draft = result.data, isLoading = false) }
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, loadFailed = true, error = result.error.message) }
            }
        }
    }

    private fun edit(change: (RecommendationPreference) -> RecommendationPreference) =
        _state.update { it.copy(draft = change(it.draft), justSaved = false, error = null) }

    fun setEnabled(enabled: Boolean) = edit { it.copy(enabled = enabled) }

    fun stepRadius(delta: Int) = edit { it.copy(radiusKm = RecommendationPreference.stepRadius(it.radiusKm, delta)) }

    fun setMinNetProfit(won: Long?) = edit { it.copy(minNetProfit = won) }

    fun toggleCargo(cargo: CargoCategory) = edit {
        it.copy(excludedCargo = if (cargo in it.excludedCargo) it.excludedCargo - cargo else it.excludedCargo + cargo)
    }

    /** 저장하지 않고 나가면 고친 값을 버린다 */
    fun discard() = _state.update { it.copy(draft = it.saved ?: it.draft, justSaved = false, error = null) }

    fun save() {
        if (_state.value.isBusy || !_state.value.hasChanges) return
        _state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.save(_state.value.draft)) {
                is ApiResult.Success -> _state.update { it.copy(saved = result.data, draft = result.data, isBusy = false, justSaved = true) }
                is ApiResult.Failure -> _state.update { it.copy(isBusy = false, error = result.error.message) }
            }
        }
    }
}
