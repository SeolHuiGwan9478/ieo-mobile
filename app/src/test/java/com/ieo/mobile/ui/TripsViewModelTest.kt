package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.trip.NewTrip
import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.domain.trip.PlaceSearch
import com.ieo.mobile.domain.trip.StopChoice
import com.ieo.mobile.domain.trip.Trip
import com.ieo.mobile.domain.trip.TripRepository
import com.ieo.mobile.domain.trip.TripStatus
import com.ieo.mobile.domain.trip.TripStop
import com.ieo.mobile.domain.trip.VisitedSite
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.trip.PlannedDay
import com.ieo.mobile.ui.trip.RegisterStep
import com.ieo.mobile.ui.trip.RegisterTripViewModel
import com.ieo.mobile.ui.trip.TripsViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TripsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val soon = Instant.parse("2026-11-09T00:00:00Z")
    private val later = Instant.parse("2026-11-10T00:00:00Z")

    private fun trip(id: Long, status: TripStatus, at: Instant? = soon, manual: Boolean = true) = Trip(
        id, status, 300_000, "인수증", null, "경기82사1234", TripStop("평택", "경기 평택시", at), TripStop("대구", "대구 달서구", null), manual, soon,
    )

    private inner class Fake : TripRepository {
        var trips = listOf(trip(1, TripStatus.SCHEDULED, later), trip(2, TripStatus.SCHEDULED, soon), trip(3, TripStatus.SCHEDULED, null), trip(4, TripStatus.IN_TRANSIT), trip(5, TripStatus.COMPLETED))
        var registered: NewTrip? = null
        var registerResult: ApiResult<Unit> = ApiResult.Success(Unit)
        val cancelled = mutableListOf<Long>()
        override suspend fun list() = ApiResult.Success(trips)
        override suspend fun visitedSites() = ApiResult.Success(listOf(VisitedSite(1, "평택", "경기 평택시")))
        override suspend fun register(trip: NewTrip): ApiResult<Unit> {
            registered = trip
            return registerResult
        }
        override suspend fun cancel(tripId: Long): ApiResult<Unit> {
            cancelled += tripId
            return ApiResult.Success(Unit)
        }
    }

    private val places = object : PlaceSearch {
        override suspend fun search(query: String) = if (query == "없는곳") emptyList() else listOf(StopChoice.New(query, "경기 화성시", 37.0, 126.9))
        override suspend fun here() = StopChoice.New("여기", "경기 김포시", 37.6, 126.7)
    }

    /** 한국 2026-11-08 12:30 */
    private val clock = Clock.fixed(Instant.parse("2026-11-08T03:30:00Z"), ZoneOffset.UTC)

    private val a = StopChoice.Known(1, "평택", "경기 평택시")
    private val b = StopChoice.Known(2, "대구", "대구 달서구")

    @Test
    fun `진행 중과 예정과 지난 운행으로 나누고 예정은 빠른 순이다`() = runTest(main.dispatcher) {
        val vm = TripsViewModel(Fake())
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals(listOf(4L), state.inProgress.map { it.id })
        assertEquals(listOf(2L, 1L, 3L), state.upcoming.map { it.id })
        assertEquals(listOf(5L), state.past.map { it.id })
        assertEquals(4L, state.next?.id)
    }

    @Test
    fun `직접 등록한 예정 운행만 취소할 수 있다`() {
        assertTrue(trip(1, TripStatus.SCHEDULED).canCancel)
        assertFalse(trip(1, TripStatus.SCHEDULED, manual = false).canCancel)
        assertFalse(trip(1, TripStatus.IN_TRANSIT).canCancel)
    }

    @Test
    fun `취소하면 상세를 닫고 새로 받는다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = TripsViewModel(fake)
        advanceUntilIdle()
        vm.open(1)
        vm.cancel()
        advanceUntilIdle()
        assertEquals(listOf(1L), fake.cancelled)
        assertNull(vm.state.value.selected)
    }

    @Test
    fun `장소를 고르면 바로 다음 단계로 간다`() = runTest(main.dispatcher) {
        val vm = RegisterTripViewModel(Fake(), places, clock)
        advanceUntilIdle()
        assertEquals("여기", vm.state.value.here?.name)
        vm.choose(a)
        assertEquals(RegisterStep.DROPOFF, vm.state.value.step)
        vm.choose(b)
        assertEquals(RegisterStep.WHEN, vm.state.value.step)
    }

    @Test
    fun `상차지와 같은 곳은 하차지로 고를 수 없다`() = runTest(main.dispatcher) {
        val vm = RegisterTripViewModel(Fake(), places, clock)
        advanceUntilIdle()
        vm.choose(a)
        vm.choose(a)
        assertEquals(RegisterStep.DROPOFF, vm.state.value.step)
        assertEquals(RegisterTripViewModel.SAME_PLACE_MESSAGE, vm.state.value.error)
    }

    @Test
    fun `주소를 찾고 못 찾으면 알려 준다`() = runTest(main.dispatcher) {
        val vm = RegisterTripViewModel(Fake(), places, clock)
        advanceUntilIdle()
        vm.onQuery("화성")
        vm.search()
        advanceUntilIdle()
        assertEquals(1, vm.state.value.results.size)
        vm.onQuery("없는곳")
        vm.search()
        advanceUntilIdle()
        assertTrue(vm.state.value.searched && vm.state.value.results.isEmpty())
    }

    @Test
    fun `시각은 다음 정시로 시작하고 운임은 만 원 단위다`() = runTest(main.dispatcher) {
        val vm = RegisterTripViewModel(Fake(), places, clock)
        advanceUntilIdle()
        assertEquals(13, vm.state.value.time.hour)
        vm.onFare("38만")
        assertEquals(380_000L, vm.state.value.fare)
        vm.addFare(5)
        assertEquals(430_000L, vm.state.value.fare)
    }

    @Test
    fun `등록하면 상차 시각을 한국 시간으로 보낸다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = RegisterTripViewModel(fake, places, clock)
        advanceUntilIdle()
        vm.choose(a)
        vm.choose(b)
        vm.selectDay(PlannedDay.TOMORROW)
        vm.shiftTime(-30)
        vm.whenDone()
        vm.onFare("45")
        vm.selectPayment(PaymentType.PREPAID)
        vm.onBroker("  ")
        vm.submit()
        advanceUntilIdle()
        val trip = fake.registered!!
        assertEquals(Instant.parse("2026-11-09T03:30:00Z"), trip.pickupAt)
        assertEquals(450_000L, trip.fare)
        assertEquals(PaymentType.PREPAID, trip.paymentType)
        assertNull(trip.brokerName)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun `시각이 미정이면 비워 보내고 실패하면 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { registerResult = ApiResult.Failure(ApiError.Network) }
        val vm = RegisterTripViewModel(fake, places, clock)
        advanceUntilIdle()
        vm.choose(a)
        vm.choose(b)
        vm.selectDay(PlannedDay.UNKNOWN)
        vm.whenDone()
        vm.onFare("10")
        vm.submit()
        advanceUntilIdle()
        assertNull(fake.registered?.pickupAt)
        assertEquals(ApiError.Network.message, vm.state.value.error)
        assertFalse(vm.state.value.done)
    }

    @Test
    fun `뒤로 가기는 한 단계씩 돌아간다`() = runTest(main.dispatcher) {
        val vm = RegisterTripViewModel(Fake(), places, clock)
        advanceUntilIdle()
        assertFalse(vm.back())
        vm.choose(a)
        assertTrue(vm.back())
        assertEquals(RegisterStep.PICKUP, vm.state.value.step)
    }
}
