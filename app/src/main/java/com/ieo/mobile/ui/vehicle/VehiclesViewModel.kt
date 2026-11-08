package com.ieo.mobile.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.vehicle.BodyType
import com.ieo.mobile.domain.vehicle.NewVehicle
import com.ieo.mobile.domain.vehicle.Tonnage
import com.ieo.mobile.domain.vehicle.Vehicle
import com.ieo.mobile.domain.vehicle.VehicleHeight
import com.ieo.mobile.domain.vehicle.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 차량 입력 칸. [editing]이 있으면 고치기라 번호는 바꾸지 못한다 */
data class VehicleForm(
    val editing: Vehicle? = null,
    val plateNumber: String = "",
    val tonnage: Tonnage = Tonnage.TON_5,
    val bodyType: BodyType = BodyType.CARGO,
    val heightMeters: Double? = null,
) {
    val canSave get() = editing != null || plateNumber.isNotBlank()
}

data class VehiclesState(
    val vehicles: List<Vehicle> = emptyList(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val form: VehicleForm? = null,
    val error: String? = null,
) {
    val defaultVehicle: Vehicle? get() = vehicles.firstOrNull { it.isDefault } ?: vehicles.firstOrNull()
}

/** 내 차량 (VEH-001, M06): 목록, 추가, 고치기, 기본 지정, 삭제 */
class VehiclesViewModel(private val vehicles: VehicleRepository) : ViewModel() {

    private val _state = MutableStateFlow(VehiclesState())
    val state: StateFlow<VehiclesState> = _state.asStateFlow()

    init {
        viewModelScope.launch { reload() }
    }

    fun openAdd() = _state.update { it.copy(form = VehicleForm(), error = null) }

    fun openEdit(vehicle: Vehicle) = _state.update {
        it.copy(form = VehicleForm(vehicle, vehicle.plateNumber, vehicle.tonnage, vehicle.bodyType, vehicle.heightMeters), error = null)
    }

    /** 입력 화면이 열려 있었으면 닫고 true */
    fun closeForm(): Boolean {
        if (_state.value.form == null) return false
        _state.update { it.copy(form = null, error = null) }
        return true
    }

    fun onPlateChange(value: String) = editForm { it.copy(plateNumber = value.take(20)) }

    fun onTonnage(value: Tonnage) = editForm { it.copy(tonnage = value) }

    fun onBodyType(value: BodyType) = editForm { it.copy(bodyType = value) }

    fun stepHeight(delta: Double) = editForm { it.copy(heightMeters = VehicleHeight.step(it.heightMeters, delta)) }

    fun clearHeight() = editForm { it.copy(heightMeters = null) }

    fun save() {
        val form = _state.value.form ?: return
        if (!form.canSave) return
        change(closeForm = true) {
            val editing = form.editing
            if (editing != null) {
                vehicles.update(editing.id, form.tonnage, form.bodyType, form.heightMeters)
            } else {
                vehicles.add(NewVehicle(form.plateNumber.trim(), form.tonnage, form.bodyType, form.heightMeters))
            }
        }
    }

    fun setDefault(vehicle: Vehicle) = change { vehicles.setDefault(vehicle.id) }

    fun delete(vehicle: Vehicle) = change { vehicles.delete(vehicle.id) }

    private fun editForm(change: (VehicleForm) -> VehicleForm) =
        _state.update { state -> state.copy(form = state.form?.let(change), error = null) }

    private fun change(closeForm: Boolean = false, block: suspend () -> ApiResult<Unit>) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is ApiResult.Success -> {
                    if (closeForm) _state.update { it.copy(form = null) }
                    reload()
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            }
            _state.update { it.copy(isBusy = false) }
        }
    }

    private suspend fun reload() {
        val result = vehicles.list()
        _state.update {
            when (result) {
                is ApiResult.Success -> it.copy(vehicles = result.data, isLoading = false)
                is ApiResult.Failure -> it.copy(isLoading = false, error = result.error.message)
            }
        }
    }
}
