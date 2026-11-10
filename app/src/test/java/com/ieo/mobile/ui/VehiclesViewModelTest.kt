package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.vehicle.BodyType
import com.ieo.mobile.domain.vehicle.NewVehicle
import com.ieo.mobile.domain.vehicle.Tonnage
import com.ieo.mobile.domain.vehicle.Vehicle
import com.ieo.mobile.domain.vehicle.VehicleDimensions
import com.ieo.mobile.domain.vehicle.VehicleHeight
import com.ieo.mobile.domain.vehicle.VehicleRepository
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.vehicle.VehiclesViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VehiclesViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val truck = Vehicle(1, "경기82사1234", Tonnage.TON_5, BodyType.CARGO, 3.6, isDefault = true)

    private inner class Fake(var saved: List<Vehicle> = listOf(truck)) : VehicleRepository {
        var result: ApiResult<Unit> = ApiResult.Success(Unit)
        var added: NewVehicle? = null
        var updatedPlate: String? = "보내지 않음"
        var updatedTonnage: Tonnage? = null
        var fuelEconomy: Double? = -1.0
        override suspend fun list() = ApiResult.Success(saved)
        override suspend fun add(vehicle: NewVehicle): ApiResult<Unit> {
            added = vehicle
            return result
        }
        override suspend fun update(vehicleId: Long, tonnage: Tonnage, bodyType: BodyType, heightMeters: Double?, dimensions: VehicleDimensions, plateNumber: String?): ApiResult<Unit> {
            updatedPlate = plateNumber
            updatedTonnage = tonnage
            return result
        }
        override suspend fun setManualFuelEconomy(vehicleId: Long, kmPerLiter: Double?): ApiResult<Unit> {
            fuelEconomy = kmPerLiter
            return ApiResult.Success(Unit)
        }
    }

    @Test
    fun `높이는 처음 누르면 3점5m이고 범위 안에서 움직인다`() {
        assertEquals(3.5, VehicleHeight.step(null, 0.1)!!, 0.001)
        assertEquals(4.2, VehicleHeight.step(4.2, 0.1)!!, 0.001)
    }

    @Test
    fun `들어가면 내 차 값으로 채워지고 바꾼 게 없으면 저장이 꺼진다`() = runTest(main.dispatcher) {
        val vm = VehiclesViewModel(Fake())
        advanceUntilIdle()
        val form = vm.state.value.form!!
        assertEquals("경기82사1234", form.plateNumber)
        assertFalse(form.canSave)
        vm.setHeight(3.7)
        assertTrue(vm.state.value.form!!.canSave)
        assertFalse(vm.state.value.form!!.identityChanged)
    }

    @Test
    fun `제원만 바꾸면 번호는 보내지 않고 저장했다고 알린다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        vm.setHeight(3.7)
        vm.save()
        advanceUntilIdle()
        assertNull(fake.updatedPlate)
        assertEquals(VehiclesViewModel.SAVED_MESSAGE, vm.state.value.message)
    }

    @Test
    fun `번호나 톤수를 바꾸면 등록증을 다시 올리라고 알린다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        vm.onPlateChange("경기82사1235")
        vm.stepTonnage(1)
        assertTrue(vm.state.value.form!!.identityChanged)
        vm.save()
        advanceUntilIdle()
        assertEquals("경기82사1235", fake.updatedPlate)
        assertEquals(Tonnage.entries[Tonnage.TON_5.ordinal + 1], fake.updatedTonnage)
        assertEquals(VehiclesViewModel.RECHECK_MESSAGE, vm.state.value.message)
    }

    @Test
    fun `차가 없으면 같은 페이지에서 등록한다`() = runTest(main.dispatcher) {
        val fake = Fake(saved = emptyList())
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        assertNull(vm.state.value.form!!.editing)
        assertFalse(vm.state.value.form!!.canSave)
        vm.onPlateChange("80가 4822")
        vm.save()
        advanceUntilIdle()
        assertEquals("80가4822", fake.added?.plateNumber)
        assertEquals(VehiclesViewModel.REGISTERED_MESSAGE, vm.state.value.message)
    }

    @Test
    fun `평소 연비를 넣으면 등록증 확인 없이 따로 저장한다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        vm.onFuelEconomy("5.55")
        assertEquals("5.5", vm.state.value.form!!.fuelEconomy)
        assertTrue(vm.state.value.form!!.canSave)
        assertFalse(vm.state.value.form!!.identityChanged)
        vm.save()
        advanceUntilIdle()
        assertEquals(5.5, fake.fuelEconomy!!, 0.001)
        assertEquals(VehiclesViewModel.SAVED_MESSAGE, vm.state.value.message)
    }

    @Test
    fun `연비를 비우면 기본값으로 돌리고 범위를 벗어나면 저장이 꺼진다`() = runTest(main.dispatcher) {
        val fake = Fake(saved = listOf(truck.copy(manualKmPerLiter = 5.5)))
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        assertEquals("5.5", vm.state.value.form!!.fuelEconomy)
        vm.onFuelEconomy("30")
        assertFalse(vm.state.value.form!!.canSave)
        vm.onFuelEconomy("")
        vm.save()
        advanceUntilIdle()
        assertNull(fake.fuelEconomy)
    }

    @Test
    fun `형식이 틀린 번호는 저장 버튼이 꺼진다`() = runTest(main.dispatcher) {
        val vm = VehiclesViewModel(Fake())
        advanceUntilIdle()
        vm.onPlateChange("12각3456")
        assertFalse(vm.state.value.form!!.canSave)
    }

    @Test
    fun `저장이 실패하면 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { result = ApiResult.Failure(ApiError.Server(409, "VEHICLE_409_PLATE", "이미 다른 계정에 등록된 번호예요")) }
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        vm.onPlateChange("80가4824")
        vm.save()
        advanceUntilIdle()
        assertEquals("이미 다른 계정에 등록된 번호예요", vm.state.value.error)
    }
}
