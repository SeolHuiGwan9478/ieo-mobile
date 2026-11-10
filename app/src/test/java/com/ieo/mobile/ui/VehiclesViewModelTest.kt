package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.vehicle.BodyType
import com.ieo.mobile.domain.vehicle.NewVehicle
import com.ieo.mobile.domain.vehicle.Tonnage
import com.ieo.mobile.domain.vehicle.Vehicle
import com.ieo.mobile.domain.vehicle.VehicleHeight
import com.ieo.mobile.domain.vehicle.VehicleRepository
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.vehicle.VehiclesViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VehiclesViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val truck = Vehicle(1, "경기82사1234", Tonnage.TON_5, BodyType.CARGO, 3.6, isDefault = true)
    private val second = Vehicle(2, "경기80바5678", Tonnage.TON_11, BodyType.WING, null, isDefault = false)

    private inner class Fake : VehicleRepository {
        var saved = listOf(truck, second)
        var result: ApiResult<Unit> = ApiResult.Success(Unit)
        val calls = mutableListOf<String>()
        var added: NewVehicle? = null
        var updated: Triple<Tonnage, BodyType, Double?>? = null
        override suspend fun list() = ApiResult.Success(saved)
        override suspend fun add(vehicle: NewVehicle): ApiResult<Unit> {
            added = vehicle
            return result
        }
        override suspend fun update(vehicleId: Long, tonnage: Tonnage, bodyType: BodyType, heightMeters: Double?, dimensions: com.ieo.mobile.domain.vehicle.VehicleDimensions): ApiResult<Unit> {
            updated = Triple(tonnage, bodyType, heightMeters)
            return result
        }
        override suspend fun setDefault(vehicleId: Long): ApiResult<Unit> {
            calls += "default$vehicleId"
            return result
        }
        override suspend fun delete(vehicleId: Long): ApiResult<Unit> {
            calls += "delete$vehicleId"
            return result
        }
    }

    @Test
    fun `높이는 처음 누르면 3점5m이고 범위 안에서 움직인다`() {
        assertEquals(3.5, VehicleHeight.step(null, 0.1), 0.001)
        assertEquals(3.7, VehicleHeight.step(3.6, 0.1), 0.001)
        assertEquals(4.2, VehicleHeight.step(4.2, 0.1), 0.001)
        assertEquals(1.5, VehicleHeight.step(1.5, -0.1), 0.001)
    }

    @Test
    fun `차량 요약은 높이를 몰라도 보여 준다`() {
        assertEquals("5톤 카고 · 높이 3.6m", truck.summary)
        assertEquals("11톤 윙바디 · 높이 모름", second.summary)
    }

    @Test
    fun `번호가 있어야 추가할 수 있고 저장하면 창을 닫고 새로 받는다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        vm.openAdd()
        assertFalse(vm.state.value.form!!.canSave)
        vm.onPlateChange(" 경기82사9999 ")
        vm.onTonnage(Tonnage.TON_8)
        vm.save()
        fake.saved = listOf(truck)
        advanceUntilIdle()
        assertEquals("경기82사9999", fake.added?.plateNumber)
        assertEquals(Tonnage.TON_8, fake.added?.tonnage)
        assertNull(vm.state.value.form)
        assertEquals(1, vm.state.value.vehicles.size)
    }

    @Test
    fun `고치기는 지금 값으로 시작하고 높이를 비울 수 있다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        vm.openEdit(truck)
        assertEquals(3.6, vm.state.value.form?.heightMeters)
        vm.onBodyType(BodyType.WING)
        vm.clearHeight()
        vm.save()
        advanceUntilIdle()
        assertEquals(Triple(Tonnage.TON_5, BodyType.WING, null), fake.updated)
    }

    @Test
    fun `저장이 실패하면 창에 남아 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { result = ApiResult.Failure(ApiError.Server(400, "VEHICLE_400_PLATE", "차량 번호 형식이 올바르지 않습니다")) }
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        vm.openAdd()
        // 앱 검사를 통과해도 서버가 막으면(예: 이미 등록된 번호) 창에 남아 이유를 보여 준다
        vm.onPlateChange("12가3456")
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.state.value.form)
        assertEquals("차량 번호 형식이 올바르지 않습니다", vm.state.value.error)
        assertFalse(vm.state.value.isBusy)
    }

    @Test
    fun `형식이 틀린 번호는 저장 버튼이 꺼진다`() = runTest(main.dispatcher) {
        val vm = VehiclesViewModel(Fake())
        advanceUntilIdle()
        vm.openAdd()
        vm.onPlateChange("12김3456")
        assertFalse(vm.state.value.form!!.canSave)
        vm.onPlateChange("12가3456")
        assertTrue(vm.state.value.form!!.canSave)
    }

    @Test
    fun `운행 기록이 있어 못 지우면 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { result = ApiResult.Failure(ApiError.Server(409, "VEHICLE_409_IN_USE", "운행 기록이 있는 차량은 지울 수 없습니다.")) }
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        vm.delete(truck)
        advanceUntilIdle()
        assertEquals("운행 기록이 있는 차량은 지울 수 없습니다.", vm.state.value.error)
    }

    @Test
    fun `기본 차량을 바꾼다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        vm.setDefault(second)
        advanceUntilIdle()
        assertEquals(listOf("default2"), fake.calls)
    }

    @Test
    fun `뒤로 가면 입력 창을 먼저 닫는다`() = runTest(main.dispatcher) {
        val vm = VehiclesViewModel(Fake())
        advanceUntilIdle()
        vm.openAdd()
        assertTrue(vm.closeForm())
        assertFalse(vm.closeForm())
    }

    @Test
    fun `기본 차량이 없으면 첫 차량을 대표로 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { saved = listOf(second) }
        val vm = VehiclesViewModel(fake)
        advanceUntilIdle()
        assertEquals(second, vm.state.value.defaultVehicle)
    }
}
