package com.ieo.mobile.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.order.LocationSource
import com.ieo.mobile.domain.order.Order
import com.ieo.mobile.domain.order.OrderProfit
import com.ieo.mobile.domain.order.OrderRepository
import com.ieo.mobile.domain.order.ReportType
import com.ieo.mobile.domain.recommendation.ExcludedCargoStore
import com.ieo.mobile.ui.common.DayTimeFormatter
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OrderSort(val label: String) { SOON("상차 시간순"), FARE("운임 높은 순") }

data class OrdersState(
    val orders: List<Order> = emptyList(),
    val conditions: OrderConditions = OrderConditions(),
    val sort: OrderSort = OrderSort.SOON,
    /** 조건 창에서 고르는 중인 조건. 창이 닫혀 있으면 null */
    val draft: OrderConditions? = null,
    /** [draft]로 찾으면 나올 건수. 세는 중이면 null */
    val draftCount: Int? = null,
    /** 위치를 알아 실제로 근처만 받았다 */
    val nearApplied: Boolean = false,
    /** 마지막으로 목록을 받은 시각. 아직 못 받았으면 null */
    val updatedAt: Instant? = null,
    /** 자동 새로고침으로 새로 들어온 오더 */
    val newIds: Set<Long> = emptySet(),
    val selectedId: Long? = null,
    /** 고른 오더의 실수익 내역. 받기 전이거나 위치를 모르면 null */
    val profit: OrderProfit? = null,
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

    /** 근처로 찾으려 했지만 위치를 몰라 전국에서 받았다 */
    val locationMissing: Boolean get() = conditions.radius.km != null && !nearApplied && !isLoading && !loadFailed
}

/**
 * 오더 찾기 (DSP-010·020·042, 시안 M08·M09): 목록, 조건, 상세, 배차 요청·철회, 신고.
 * 목록은 화면이 보이는 동안 1분마다 다시 받는다 ([autoRefresh]). 상세나 조건 창이 열려 있으면 순서가 바뀌지 않게 닫은 뒤에 받는다
 */
class OrdersViewModel(
    private val repository: OrderRepository,
    private val location: LocationSource,
    private val clock: Clock = Clock.system(DayTimeFormatter.KOREA),
    /** 제외할 화물은 배차 추천 설정과 같이 쓴다 */
    private val excludedCargo: ExcludedCargoStore = ExcludedCargoStore.None,
) : ViewModel() {

    private val labels = DayTimeFormatter(clock)
    private val _state = MutableStateFlow(OrdersState())
    val state: StateFlow<OrdersState> = _state.asStateFlow()

    private var countJob: Job? = null
    /** 상세·조건 창 때문에 미뤄 둔 자동 새로고침 */
    private var refreshPending = false

    /** 화면에 보일 오더: 받은 목록에 날짜·제외할 화물을 적용하고 정렬한다 */
    fun visible(state: OrdersState): List<Order> {
        val filtered = state.orders.filter { matches(it, state.conditions) }
        return when (state.sort) {
            OrderSort.SOON -> filtered.sortedBy { it.pickupFrom }
            OrderSort.FARE -> filtered.sortedByDescending { it.fare }
        }
    }

    /** 처음 열 때, 새로고침 버튼, 아래로 당기기. 배차 추천에서 바꾼 제외할 화물도 이때 다시 읽는다 */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = null) }
            excludedCargo.load()?.let { saved -> _state.update { it.copy(conditions = it.conditions.copy(excluded = saved)) } }
            reload(markNew = false)
            _state.update { it.copy(isLoading = false) }
        }
    }

    /** 1분마다 화면이 부른다. 로딩 표시 없이 받고, 새로 들어온 오더에 표시를 붙인다 */
    fun autoRefresh() {
        val now = _state.value
        if (now.selectedId != null || now.draft != null) {
            refreshPending = true
            return
        }
        viewModelScope.launch { reload(markNew = true) }
    }

    fun toggleSort() = _state.update { it.copy(sort = if (it.sort == OrderSort.SOON) OrderSort.FARE else OrderSort.SOON) }

    fun openConditions() {
        _state.update { it.copy(draft = it.conditions, draftCount = null) }
        countDraft()
    }

    fun editDraft(change: (OrderConditions) -> OrderConditions) {
        val draft = _state.value.draft ?: return
        _state.update { it.copy(draft = change(draft), draftCount = null) }
        countDraft()
    }

    fun resetDraft() = editDraft { OrderConditions() }

    fun closeConditions() {
        countJob?.cancel()
        _state.update { it.copy(draft = null, draftCount = null) }
        runPendingRefresh()
    }

    /** 고른 조건으로 목록을 다시 받는다. 조건이 바뀌었으니 "새로 올라옴" 표시는 지운다 */
    fun applyConditions() {
        val draft = _state.value.draft ?: return
        countJob?.cancel()
        refreshPending = false
        val excludedChanged = draft.excluded != _state.value.conditions.excluded
        _state.update { it.copy(conditions = draft, draft = null, draftCount = null, newIds = emptySet()) }
        if (excludedChanged) {
            // 저장한 뒤에 다시 받아야 refresh가 옛 값으로 되돌리지 않는다
            viewModelScope.launch {
                excludedCargo.save(draft.excluded)
                refresh()
            }
        } else {
            refresh()
        }
    }

    fun open(orderId: Long) {
        _state.update { it.copy(selectedId = orderId, profit = null, reporting = false, message = null) }
        viewModelScope.launch {
            val from = location.lastKnown() ?: return@launch
            val loaded = (repository.profit(orderId, from) as? ApiResult.Success)?.data ?: return@launch
            _state.update { if (it.selectedId == orderId) it.copy(profit = loaded) else it }
        }
    }

    fun close() {
        _state.update { it.copy(selectedId = null, profit = null, reporting = false, message = null) }
        runPendingRefresh()
    }

    fun request() {
        val order = _state.value.selected ?: return
        action {
            when (val result = repository.request(order.id)) {
                is ApiResult.Success -> {
                    show(REQUESTED_MESSAGE)
                    reload(markNew = false)
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
                    reload(markNew = false)
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
            val result = repository.report(order.id, type)
            val already = (result as? ApiResult.Failure)?.error.let { it is ApiError.Server && it.code == ALREADY_REPORTED_CODE }
            if (result is ApiResult.Success || already) {
                // 신고는 오더마다 한 번이라 신고 버튼을 닫는다
                _state.update { s -> s.copy(reporting = false, orders = s.orders.map { if (it.id == order.id) it.copy(reportedByMe = true) else it }) }
            }
            when {
                result is ApiResult.Success -> show(REPORTED_MESSAGE)
                result is ApiResult.Failure -> fail(result.error.message)
            }
        }
    }

    /** 버튼 위에 잠깐 띄운 안내를 지운다. 실패 안내는 다음 동작까지 둔다 */
    fun clearMessage() = _state.update { if (it.messageIsError) it else it.copy(message = null) }

    private fun runPendingRefresh() {
        if (!refreshPending) return
        refreshPending = false
        autoRefresh()
    }

    /** 조건 창 아래 버튼에 넣을 건수. 고를 때마다 앞의 셈은 버린다 */
    private fun countDraft() {
        val draft = _state.value.draft ?: return
        countJob?.cancel()
        countJob = viewModelScope.launch {
            val near = draft.radius.km?.let { location.lastKnown() }
            val result = repository.openOrders(LIST_SIZE, near, draft.search)
            val count = (result as? ApiResult.Success)?.data?.count { matches(it, draft) } ?: return@launch
            _state.update { if (it.draft == draft) it.copy(draftCount = count) else it }
        }
    }

    private fun matches(order: Order, conditions: OrderConditions): Boolean {
        val date = order.pickupFrom.atZone(DayTimeFormatter.KOREA).toLocalDate()
        val dayOk = when (conditions.day) {
            DayFilter.ALL -> true
            DayFilter.TODAY -> date == labels.today()
            DayFilter.TOMORROW -> date == labels.today().plusDays(1)
        }
        return dayOk && order.cargoCategory !in conditions.excluded
    }

    private suspend fun reload(markNew: Boolean) {
        val conditions = _state.value.conditions
        val near = conditions.radius.km?.let { location.lastKnown() }
        when (val result = repository.openOrders(LIST_SIZE, near, conditions.search)) {
            is ApiResult.Success -> _state.update { current ->
                val before = current.orders.map { it.id }.toSet()
                val arrived = if (markNew && before.isNotEmpty()) result.data.map { it.id }.filterNot { it in before }.toSet() else emptySet()
                current.copy(
                    orders = result.data,
                    nearApplied = near != null,
                    loadFailed = false,
                    updatedAt = clock.instant(),
                    // 한 번 붙은 표시는 그 오더가 목록에 있는 동안 둔다
                    newIds = (current.newIds + arrived).filter { id -> result.data.any { it.id == id } }.toSet(),
                )
            }
            is ApiResult.Failure -> {
                _state.update { it.copy(loadFailed = it.orders.isEmpty()) }
                // 자동 새로고침 실패는 조용히 넘긴다. 다음 1분에 다시 받는다
                if (!markNew) fail(result.error.message)
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
        const val AUTO_REFRESH_MILLIS = 60_000L
        const val REQUESTED_MESSAGE = "배차를 요청했어요. 주선사가 확정하면 배차가 잡혀요"
        const val WITHDRAWN_MESSAGE = "요청을 철회했어요"
        const val REPORTED_MESSAGE = "신고를 접수했어요"
        private const val ALREADY_REPORTED_CODE = "ORDER_409_REPORTED"
    }
}
