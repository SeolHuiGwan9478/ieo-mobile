package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.notification.AppNotification
import com.ieo.mobile.domain.notification.NotificationRepository
import com.ieo.mobile.domain.notification.NotificationSeenStore
import com.ieo.mobile.domain.notification.NotificationStreamEvent
import com.ieo.mobile.domain.notification.NotificationType
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.notification.NotificationsViewModel
import com.ieo.mobile.ui.notification.timeAgo
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotificationsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private fun note(id: Long) = AppNotification(id, NotificationType.DISPATCH_CONFIRMED, "m$id", id, Instant.EPOCH)

    private class Store(override var lastSeenId: Long? = null) : NotificationSeenStore

    private class Fake(var recent: List<AppNotification> = emptyList()) : NotificationRepository {
        val live = MutableSharedFlow<NotificationStreamEvent>()
        val afters = mutableListOf<Long?>()
        var failFirst = false
        override suspend fun recent() = ApiResult.Success(recent)
        override fun stream(after: Long?): Flow<NotificationStreamEvent> {
            afters += after
            if (failFirst) {
                failFirst = false
                return flow { throw IOException() }
            }
            return live
        }
    }

    @Test
    fun `처음 쓰는 기기는 지난 알림으로 배지를 채우지 않는다`() = runTest(main.dispatcher) {
        val store = Store()
        val vm = NotificationsViewModel(Fake(listOf(note(1), note(2))), store)
        vm.load()
        advanceUntilIdle()
        assertEquals(0, vm.state.value.unreadCount)
        assertEquals(2L, store.lastSeenId)
    }

    @Test
    fun `본 뒤에 온 알림만 센다`() = runTest(main.dispatcher) {
        val vm = NotificationsViewModel(Fake(listOf(note(1), note(2), note(3))), Store(1))
        vm.load()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.unreadCount)
    }

    @Test
    fun `열면 배지는 없어지고 처음 보는 줄은 표시가 남는다`() = runTest(main.dispatcher) {
        val store = Store(1)
        val vm = NotificationsViewModel(Fake(listOf(note(1), note(2))), store)
        vm.load()
        advanceUntilIdle()
        vm.open()
        advanceUntilIdle()
        assertEquals(0, vm.state.value.unreadCount)
        assertEquals(2L, store.lastSeenId)
        assertTrue(vm.state.value.isNew(note(2)))
        assertEquals(false, vm.state.value.isNew(note(1)))
    }

    @Test
    fun `실시간 알림은 배너와 배지가 되고 같은 알림은 한 번만 친다`() = runTest(main.dispatcher) {
        val fake = Fake(listOf(note(1)))
        val vm = NotificationsViewModel(fake, Store(1))
        val job = launch { vm.listen() }
        runCurrent()
        assertEquals(1L, fake.afters.single())
        fake.live.emit(NotificationStreamEvent.Received(note(2)))
        fake.live.emit(NotificationStreamEvent.Received(note(2)))
        runCurrent()
        assertEquals(listOf(2L, 1L), vm.state.value.items.map { it.id })
        assertEquals(1, vm.state.value.unreadCount)
        assertEquals(2L, vm.state.value.banner?.id)
        job.cancel()
    }

    @Test
    fun `알림함을 보고 있으면 배너 없이 읽은 것이 된다`() = runTest(main.dispatcher) {
        val fake = Fake(listOf(note(1)))
        val vm = NotificationsViewModel(fake, Store(1))
        val job = launch { vm.listen() }
        runCurrent()
        vm.open()
        runCurrent()
        fake.live.emit(NotificationStreamEvent.Received(note(2)))
        runCurrent()
        assertNull(vm.state.value.banner)
        assertEquals(0, vm.state.value.unreadCount)
        job.cancel()
    }

    @Test
    fun `연결이 실패하면 기다렸다 놓친 다음부터 다시 붙는다`() = runTest(main.dispatcher) {
        val fake = Fake(listOf(note(5))).apply { failFirst = true }
        val vm = NotificationsViewModel(fake, Store(5))
        val job = launch { vm.listen() }
        runCurrent()
        assertEquals(1, fake.afters.size)
        advanceTimeBy(NotificationsViewModel.RETRY_DELAYS_MS.first() + 1)
        runCurrent()
        assertEquals(listOf<Long?>(5, 5), fake.afters)
        job.cancel()
    }

    @Test
    fun `시각을 쉬운 말로 쓴다`() {
        val now = Instant.parse("2026-11-08T12:00:00Z")
        assertEquals("방금", timeAgo(now.minusSeconds(10), now, ZoneOffset.UTC))
        assertEquals("5분 전", timeAgo(now.minusSeconds(300), now, ZoneOffset.UTC))
        assertEquals("2시간 전", timeAgo(now.minusSeconds(7200), now, ZoneOffset.UTC))
        assertEquals("11월 6일", timeAgo(now.minusSeconds(172800), now, ZoneOffset.UTC))
    }
}
