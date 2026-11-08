package com.ieo.mobile.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.trip.NewTrip
import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.domain.trip.PlaceSearch
import com.ieo.mobile.domain.trip.StopChoice
import com.ieo.mobile.domain.trip.TripRepository
import com.ieo.mobile.ui.common.DayTimeFormatter
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RegisterStep { PICKUP, DROPOFF, WHEN, MONEY }

enum class PlannedDay(val label: String) { TODAY("오늘"), TOMORROW("내일"), UNKNOWN("미정") }

data class RegisterState(
    val step: RegisterStep = RegisterStep.PICKUP,
    val visited: List<StopChoice.Known> = emptyList(),
    val here: StopChoice.New? = null,
    val query: String = "",
    val results: List<StopChoice.New> = emptyList(),
    val searched: Boolean = false,
    val isSearching: Boolean = false,
    val pickup: StopChoice? = null,
    val dropoff: StopChoice? = null,
    val day: PlannedDay = PlannedDay.TODAY,
    val time: LocalTime = LocalTime.of(9, 0),
    /** 만 원 단위 */
    val fareManwon: String = "",
    val paymentType: PaymentType = PaymentType.RECEIPT,
    val brokerName: String = "",
    val isBusy: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
) {
    val fare: Long get() = (fareManwon.toLongOrNull() ?: 0) * 10_000
    val canSubmit: Boolean get() = fare > 0 && !isBusy
}

/**
 * 다른 앱에서 받은 배차 등록 (TRIP-001). 한 화면에 하나씩 묻는다:
 * 상차지 → 하차지 → 상차 시각 → 운임. 상하차지는 다녀온 곳, 지금 여기, 주소 찾기로 고른다
 */
class RegisterTripViewModel(
    private val repository: TripRepository,
    private val places: PlaceSearch,
    private val clock: Clock = Clock.system(DayTimeFormatter.KOREA),
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState(time = nextHour()))
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val visited = (repository.visitedSites() as? ApiResult.Success)?.data.orEmpty()
                .map { StopChoice.Known(it.siteId, it.name, it.address) }
            _state.update { it.copy(visited = visited) }
            places.here()?.let { here -> _state.update { it.copy(here = here) } }
        }
    }

    fun onQuery(value: String) = _state.update { it.copy(query = value.take(60), searched = false) }

    fun search() {
        val query = _state.value.query.trim()
        if (query.isEmpty()) return
        _state.update { it.copy(isSearching = true) }
        viewModelScope.launch {
            val found = places.search(query)
            _state.update { it.copy(results = found, searched = true, isSearching = false) }
        }
    }

    /** 고르면 바로 다음 단계로 간다 */
    fun choose(place: StopChoice) = _state.update {
        when (it.step) {
            RegisterStep.PICKUP -> it.clearSearch().copy(pickup = place, step = RegisterStep.DROPOFF)
            RegisterStep.DROPOFF ->
                if (place.address == it.pickup?.address) it.copy(error = SAME_PLACE_MESSAGE)
                else it.clearSearch().copy(dropoff = place, step = RegisterStep.WHEN)
            else -> it
        }
    }

    fun selectDay(day: PlannedDay) = _state.update { it.copy(day = day) }

    fun shiftTime(minutes: Long) = _state.update { it.copy(time = it.time.plusMinutes(minutes)) }

    fun whenDone() = _state.update { it.copy(step = RegisterStep.MONEY) }

    fun onFare(value: String) = _state.update { it.copy(fareManwon = value.filter(Char::isDigit).take(4), error = null) }

    fun addFare(manwon: Long) = _state.update {
        it.copy(fareManwon = ((it.fareManwon.toLongOrNull() ?: 0) + manwon).coerceAtMost(9_999).toString(), error = null)
    }

    fun selectPayment(type: PaymentType) = _state.update { it.copy(paymentType = type) }

    fun onBroker(value: String) = _state.update { it.copy(brokerName = value.take(50)) }

    /** 뒤로 갈 단계가 있으면 true */
    fun back(): Boolean {
        val previous = when (_state.value.step) {
            RegisterStep.PICKUP -> return false
            RegisterStep.DROPOFF -> RegisterStep.PICKUP
            RegisterStep.WHEN -> RegisterStep.DROPOFF
            RegisterStep.MONEY -> RegisterStep.WHEN
        }
        _state.update { it.clearSearch().copy(step = previous) }
        return true
    }

    fun submit() {
        val current = _state.value
        val pickup = current.pickup ?: return
        val dropoff = current.dropoff ?: return
        if (!current.canSubmit) return
        _state.update { it.copy(isBusy = true, error = null) }
        viewModelScope.launch {
            val trip = NewTrip(pickup, dropoff, plannedAt(current), current.fare, current.paymentType, current.brokerName.trim().ifEmpty { null })
            when (val result = repository.register(trip)) {
                is ApiResult.Success -> _state.update { it.copy(isBusy = false, done = true) }
                is ApiResult.Failure -> _state.update { it.copy(isBusy = false, error = result.error.message) }
            }
        }
    }

    private fun RegisterState.clearSearch() = copy(query = "", results = emptyList(), searched = false, error = null)

    private fun plannedAt(state: RegisterState): Instant? {
        val today = clock.instant().atZone(DayTimeFormatter.KOREA).toLocalDate()
        val date = when (state.day) {
            PlannedDay.UNKNOWN -> return null
            PlannedDay.TODAY -> today
            PlannedDay.TOMORROW -> today.plusDays(1)
        }
        return date.atTime(state.time).atZone(DayTimeFormatter.KOREA).toInstant()
    }

    private fun nextHour(): LocalTime =
        clock.instant().atZone(DayTimeFormatter.KOREA).toLocalTime().withMinute(0).withSecond(0).withNano(0).plusHours(1)

    companion object {
        const val SAME_PLACE_MESSAGE = "상차지와 다른 곳을 골라 주세요"
    }
}
