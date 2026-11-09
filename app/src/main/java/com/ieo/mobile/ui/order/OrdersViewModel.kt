package com.ieo.mobile.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.order.LocationSource
import com.ieo.mobile.domain.order.Order
import com.ieo.mobile.domain.order.OrderRepository
import com.ieo.mobile.domain.order.ReportType
import com.ieo.mobile.ui.common.DayTimeFormatter
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DayFilter(val label: String) { ALL("전체 날짜"), TODAY("오늘"), TOMORROW("내일") }

enum class OrderSort(val label: String) { SOON("상차 빠른 순"), FARE("운임 높은 순") }

data class OrdersState(
    val orders: List<Order> = emptyList(),
    val day: DayFilter = DayFilter.ALL,
    val sort: OrderSort = OrderSort.SOON,
    val prepaidOnly: Boolean = false,
    /** 내 위치 근처만 찾는다 (서버 기준 50km) */
    val nearMe: Boolean = true,
    /** 위치를 알아 실제로 근처만 받았다 */
    val nearApplied: Boolean = false,
    val selectedId: Long? = null,
    val reporting: Boolean = false,
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val message: String? = null,
    /** [message]가 실패 안내다 */
    val messageIsError: Boolean = false,
    /** 목록을 처음부터 못 받았다 */
    val loadFailed: Boolean = false,
) {
    val selected: Order? get() = selectedId?.let { id -> orders.firstOrNull { it.id == id } }

    /** 근처로 찾으려 했지만 위치를 몰라 전체를 받았다 */
    val locationMissing: Boolean get() = nearMe && !nearApplied && !isLoading && !loadFailed
}

/** 오더 찾기 (DSP-010·020·042, 시안 M08·M09): 목록, 상세, 배차 요청·철회, 신고 */
class OrdersViewModel(
    private val repository: OrderRepository,
    private val location: LocationSource,
    clock: Clock = Clock.system(DayTimeFormatter.KOREA),
) : ViewModel() {

    private val labels = DayTimeFormatter(clock)
    private val _state = MutableStateFlow(OrdersState())
    val state: StateFlow<OrdersState> = _state.asStateFlow()

    /** 화면에 보일 오더: 고른 조건과 정렬을 적용한다 */
    fun visible(state: OrdersState): List<Order> {
        val filtered = state.orders.filter { order ->
            matchesDay(order, state.day) && (!state.prepaidOnly || order.paymentTypeLabel == PREPAID)
        }
        return when (state.sort) {
            OrderSort.SOON -> filtered.sortedBy { it.pickupFrom }
            OrderSort.FARE -> filtered.sortedByDescending { it.fare }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            reload()
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun selectDay(day: DayFilter) = _state.update { it.copy(day = day) }

    fun toggleSort() = _state.update { it.copy(sort = if (it.sort == OrderSort.SOON) OrderSort.FARE else OrderSort.SOON) }

    fun togglePrepaid() = _state.update { it.copy(prepaidOnly = !it.prepaidOnly) }

    fun toggleNearMe() {
        // 위치를 못 받아 꺼져 보일 때 누르면 위치를 다시 찾는다
        if (_state.value.locationMissing) return refresh()
        _state.update { it.copy(nearMe = !it.nearMe) }
        refresh()
    }

    fun open(orderId: Long) = _state.update { it.copy(selectedId = orderId, reporting = false, message = null) }

    fun close() = _state.update { it.copy(selectedId = null, reporting = false, message = null) }

    fun request() {
        val order = _state.value.selected ?: return
        action {
            when (val result = repository.request(order.id)) {
                is ApiResult.Success -> {
                    show(REQUESTED_MESSAGE)
                    reload()
                }
                is ApiResult.Failure -> fail(result.error.message)
            }
        }
    }

    fun withdraw() {
        val request = _state.value.selected?.myRequest ?: return
        action {
            when (val result = repository.withdraw(request.id)) {
                is ApiResult.Success -> {
                    show(WITHDRAWN_MESSAGE)
                    reload()
                }
                is ApiResult.Failure -> fail(result.error.message)
            }
        }
    }

    fun startReport() = _state.update { it.copy(reporting = true) }

    fun cancelReport() = _state.update { it.copy(reporting = false) }

    fun report(type: ReportType) {
        val order = _state.value.selected ?: return
        action {
            when (val result = repository.report(order.id, type)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(reporting = false) }
                    show(REPORTED_MESSAGE)
                }
                is ApiResult.Failure -> fail(result.error.message)
            }
        }
    }

    private fun matchesDay(order: Order, day: DayFilter): Boolean {
        val date = order.pickupFrom.atZone(DayTimeFormatter.KOREA).toLocalDate()
        return when (day) {
            DayFilter.ALL -> true
            DayFilter.TODAY -> date == labels.today()
            DayFilter.TOMORROW -> date == labels.today().plusDays(1)
        }
    }

    private suspend fun reload() {
        val near = if (_state.value.nearMe) location.lastKnown() else null
        when (val result = repository.openOrders(LIST_SIZE, near)) {
            is ApiResult.Success -> _state.update { it.copy(orders = result.data, nearApplied = near != null, loadFailed = false) }
            is ApiResult.Failure -> {
                _state.update { it.copy(loadFailed = true) }
                fail(result.error.message)
            }
        }
    }

    private fun show(message: String) = _state.update { it.copy(message = message, messageIsError = false) }

    private fun fail(message: String) = _state.update { it.copy(message = message, messageIsError = true) }

    private fun action(block: suspend () -> Unit) {
        if (_state.value.isBusy) return
        _state.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    companion object {
        private const val LIST_SIZE = 50
        private const val PREPAID = "선불"
        const val REQUESTED_MESSAGE = "배차를 요청했어요. 주선사가 확정하면 배차가 잡혀요"
        const val WITHDRAWN_MESSAGE = "요청을 철회했어요"
        const val REPORTED_MESSAGE = "신고를 접수했어요"
    }
}
