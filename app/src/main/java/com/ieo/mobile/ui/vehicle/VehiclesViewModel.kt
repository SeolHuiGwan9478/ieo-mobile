package com.ieo.mobile.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.vehicle.BodyType
import com.ieo.mobile.domain.vehicle.NewVehicle
import com.ieo.mobile.domain.vehicle.PlateNumber
import com.ieo.mobile.domain.vehicle.Tonnage
import com.ieo.mobile.domain.vehicle.Vehicle
import com.ieo.mobile.domain.vehicle.VehicleDimensions
import com.ieo.mobile.domain.vehicle.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 내 차량 입력 칸. [editing]이 있으면 그 차를 고치고, 없으면 처음 등록한다 */
data class VehicleForm(
    val editing: Vehicle? = null,
    val plateNumber: String = "",
    val tonnage: Tonnage = Tonnage.TON_5,
    val bodyType: BodyType = BodyType.CARGO,
    val heightMeters: Double? = null,
    val dimensions: VehicleDimensions = VehicleDimensions(),
) {
    private val plate: String get() = PlateNumber.normalize(plateNumber)

    /** 자동차등록증에 적힌 값(번호·톤수·차종)이 바뀌었다. 저장하면 등록증을 다시 받는다 */
    val identityChanged: Boolean
        get() = editing != null && (plate != editing.plateNumber || tonnage != editing.tonnage || bodyType != editing.bodyType)

    val changed: Boolean
        get() = editing == null || identityChanged || heightMeters != editing.heightMeters || dimensions != editing.dimensions

    val canSave: Boolean get() = PlateNumber.isValid(plateNumber) && changed

    companion object {
        fun of(vehicle: Vehicle) =
            VehicleForm(vehicle, vehicle.plateNumber, vehicle.tonnage, vehicle.bodyType, vehicle.heightMeters, vehicle.dimensions)
    }
}

data class VehiclesState(
    val vehicles: List<Vehicle> = emptyList(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    /** 내 차 한 대의 입력 칸. 불러온 뒤에는 늘 있다 */
    val form: VehicleForm? = null,
    val message: String? = null,
    val error: String? = null,
) {
    val defaultVehicle: Vehicle? get() = vehicles.firstOrNull { it.isDefault } ?: vehicles.firstOrNull()
}

/**
 * 내 차량 (VEH-001): 차주 한 명에 차 한 대. 들어가면 바로 고치는 한 페이지이고, 없으면 같은 페이지에서 등록한다.
 * 번호·톤수·차종을 바꾸면 서버가 자동차등록증을 다시 받는다 (그동안 미인증)
 */
class VehiclesViewModel(private val vehicles: VehicleRepository) : ViewModel() {

    private val _state = MutableStateFlow(VehiclesState())
    val state: StateFlow<VehiclesState> = _state.asStateFlow()

    init {
        viewModelScope.launch { reload() }
    }

    /** 페이지를 열 때마다 저장된 값으로 다시 채운다 */
    fun open() {
        _state.update { it.copy(form = it.defaultVehicle?.let(VehicleForm::of) ?: VehicleForm(), message = null, error = null) }
    }

    fun onPlateChange(value: String) = editForm { it.copy(plateNumber = value.take(20)) }

    fun onTonnage(value: Tonnage) = editForm { it.copy(tonnage = value) }

    fun stepTonnage(delta: Int) = editForm {
        val all = Tonnage.entries
        it.copy(tonnage = all[(all.indexOf(it.tonnage) + delta).coerceIn(0, all.lastIndex)])
    }

    fun onBodyType(value: BodyType) = editForm { it.copy(bodyType = value) }

    fun setHeight(value: Double?) = editForm { it.copy(heightMeters = value) }

    fun onDimensions(dimensions: VehicleDimensions) = editForm { it.copy(dimensions = dimensions) }

    fun save() {
        val form = _state.value.form ?: return
        if (!form.canSave || _state.value.isBusy) return
        _state.update { it.copy(isBusy = true, error = null, message = null) }
        viewModelScope.launch {
            val plate = PlateNumber.normalize(form.plateNumber)
            val editing = form.editing
            val result = if (editing != null) {
                vehicles.update(
                    editing.id, form.tonnage, form.bodyType, form.heightMeters, form.dimensions,
                    plateNumber = plate.takeIf { it != editing.plateNumber },
                )
            } else {
                vehicles.add(NewVehicle(plate, form.tonnage, form.bodyType, form.heightMeters, form.dimensions))
            }
            when (result) {
                is ApiResult.Success -> {
                    reload()
                    _state.update {
                        it.copy(
                            form = it.defaultVehicle?.let(VehicleForm::of),
                            message = when {
                                editing == null -> REGISTERED_MESSAGE
                                form.identityChanged -> RECHECK_MESSAGE
                                else -> SAVED_MESSAGE
                            },
                        )
                    }
                }
                is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            }
            _state.update { it.copy(isBusy = false) }
        }
    }

    private fun editForm(change: (VehicleForm) -> VehicleForm) =
        _state.update { state -> state.copy(form = state.form?.let(change), error = null, message = null) }

    private suspend fun reload() {
        val result = vehicles.list()
        _state.update {
            when (result) {
                is ApiResult.Success -> {
                    val loaded = it.copy(vehicles = result.data, isLoading = false)
                    // 처음 불러올 때만 칸을 채운다. 고치는 중이면 그대로 둔다
                    if (it.form == null) loaded.copy(form = loaded.defaultVehicle?.let(VehicleForm::of) ?: VehicleForm()) else loaded
                }
                is ApiResult.Failure -> it.copy(isLoading = false, error = result.error.message)
            }
        }
    }

    companion object {
        const val SAVED_MESSAGE = "저장했어요"
        const val REGISTERED_MESSAGE = "내 차를 등록했어요"
        const val RECHECK_MESSAGE = "저장했어요. 바뀐 차의 자동차등록증을 다시 올려 주세요"
    }
}
