package com.ieo.mobile.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.notification.AppNotification
import com.ieo.mobile.domain.notification.NotificationRepository
import com.ieo.mobile.domain.notification.NotificationSeenStore
import com.ieo.mobile.domain.notification.NotificationStreamEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

data class NotificationsState(
    /** 새것부터 */
    val items: List<AppNotification> = emptyList(),
    val lastSeenId: Long? = null,
    /** 알림함을 여는 순간의 lastSeenId. 배지는 바로 없애도, 이번에 처음 보는 줄은 표시를 남긴다 */
    val highlightAfter: Long? = null,
    val isOpen: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    /** 지금 도착해 위에 잠깐 띄울 알림 */
    val banner: AppNotification? = null,
    /** 배너나 푸시 알림을 눌러 그 화면으로 가야 하는 알림. 화면이 옮긴 뒤 [NotificationsViewModel.opened]로 지운다 */
    val opening: AppNotification? = null,
) {
    val unreadCount: Int get() = lastSeenId?.let { seen -> items.count { it.id > seen } } ?: 0

    fun isNew(item: AppNotification): Boolean = highlightAfter?.let { item.id > it } ?: false
}

/** 앱이 보이는 동안 서버 알림을 받고, 어디까지 봤는지 센다 (09 알림) */
class NotificationsViewModel(
    private val repository: NotificationRepository,
    private val store: NotificationSeenStore,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState(lastSeenId = store.lastSeenId))
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    private val _received = MutableSharedFlow<AppNotification>(extraBufferCapacity = 16)

    /** 새 알림이 왔다. 배차·오더 목록을 새로 받는 데 쓴다 */
    val received: SharedFlow<AppNotification> = _received.asSharedFlow()

    fun load() {
        viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        val newestBefore = _state.value.items.firstOrNull()?.id
        _state.update { it.copy(isLoading = it.items.isEmpty(), error = null) }
        when (val result = repository.recent()) {
            is ApiResult.Success -> {
                _state.update { state -> merge(state, result.data).copy(isLoading = false) }
                // 처음 쓰는 기기: 지난 알림으로 배지를 채우지 않는다
                if (store.lastSeenId == null) markSeen(_state.value.items.firstOrNull()?.id ?: 0)
                else if (_state.value.isOpen) markSeen(_state.value.items.firstOrNull()?.id ?: return)
                // 앱을 내려 둔 사이에 온 알림: 목록도 새로 받게 알린다
                val newest = _state.value.items.firstOrNull()
                if (newestBefore != null && newest != null && newest.id > newestBefore) _received.tryEmit(newest)
            }
            is ApiResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error.message) }
        }
    }

    /** 앱이 보이는 동안만 부른다. 놓친 알림을 먼저 받고, 끊기면 점점 오래 기다렸다 다시 붙는다 */
    suspend fun listen() {
        fetch()
        var failures = 0
        while (currentCoroutineContext().isActive) {
            try {
                repository.stream(_state.value.items.firstOrNull()?.id).collect { event ->
                    when (event) {
                        NotificationStreamEvent.Connected -> failures = 0
                        is NotificationStreamEvent.Received -> arrive(event.notification)
                    }
                }
                delay(RECONNECT_DELAY_MS)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                delay(RETRY_DELAYS_MS[failures.coerceAtMost(RETRY_DELAYS_MS.lastIndex)])
                failures++
            }
        }
    }

    private fun arrive(notification: AppNotification) {
        if (_state.value.items.any { it.id == notification.id }) return
        _state.update { merge(it, listOf(notification)).copy(banner = notification.takeUnless { _ -> it.isOpen }) }
        if (_state.value.isOpen) markSeen(notification.id)
        _received.tryEmit(notification)
    }

    private fun merge(state: NotificationsState, incoming: List<AppNotification>) =
        state.copy(items = (state.items + incoming).distinctBy { it.id }.sortedByDescending { it.id }.take(MAX_ITEMS))

    fun open() {
        val newest = _state.value.items.firstOrNull()?.id
        _state.update { it.copy(isOpen = true, highlightAfter = it.lastSeenId, banner = null) }
        newest?.let(::markSeen)
        load()
    }

    fun close() = _state.update { it.copy(isOpen = false, highlightAfter = null) }

    fun dismissBanner() = _state.update { it.copy(banner = null) }

    /** 배너를 눌렀다. 누른 것은 사용자가 고른 것이라 그 화면으로 옮긴다 */
    fun openBanner(notification: AppNotification) {
        markSeen(maxOf(notification.id, _state.value.lastSeenId ?: 0))
        _state.update { it.copy(banner = null, opening = notification) }
    }

    fun opened() = _state.update { it.copy(opening = null) }

    private fun markSeen(id: Long) {
        store.lastSeenId = id
        _state.update { it.copy(lastSeenId = id) }
    }

    companion object {
        const val RECONNECT_DELAY_MS = 1_000L
        val RETRY_DELAYS_MS = listOf(2_000L, 5_000L, 10_000L, 30_000L, 60_000L)
        private const val MAX_ITEMS = 50
    }
}
