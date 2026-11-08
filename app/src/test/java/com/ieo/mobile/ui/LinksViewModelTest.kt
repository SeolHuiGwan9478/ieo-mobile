package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.link.DriverLink
import com.ieo.mobile.domain.link.DriverLinkRepository
import com.ieo.mobile.domain.link.DriverLinkStatus
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.link.LinksViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LinksViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val invite = DriverLink(1, "한빛물류", DriverLinkStatus.PENDING, null, null)
    private val linked = DriverLink(1, "한빛물류", DriverLinkStatus.LINKED, null, null)

    private inner class Fake : DriverLinkRepository {
        var pending = listOf(invite)
        var active = emptyList<DriverLink>()
        var acceptResult: ApiResult<DriverLink> = ApiResult.Success(linked)
        val calls = mutableListOf<String>()
        override suspend fun invitations() = ApiResult.Success(pending)
        override suspend fun links() = ApiResult.Success(active)
        override suspend fun accept(linkId: Long): ApiResult<DriverLink> {
            calls += "accept$linkId"
            if (acceptResult is ApiResult.Success) {
                pending = emptyList()
                active = listOf(linked)
            } else {
                pending = emptyList()
            }
            return acceptResult
        }
        override suspend fun decline(linkId: Long): ApiResult<Unit> {
            calls += "decline$linkId"
            pending = emptyList()
            return ApiResult.Success(Unit)
        }
        override suspend fun unlink(linkId: Long): ApiResult<Unit> {
            calls += "unlink$linkId"
            active = emptyList()
            return ApiResult.Success(Unit)
        }
    }

    @Test
    fun `수락하면 연결된 회사로 옮겨 간다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = LinksViewModel(fake)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.invitations.size)
        vm.accept(invite)
        advanceUntilIdle()
        assertTrue(vm.state.value.invitations.isEmpty())
        assertEquals(1, vm.state.value.links.size)
        assertEquals("한빛물류 연결을 마쳤어요", vm.state.value.notice)
    }

    @Test
    fun `이미 만료된 요청이면 이유를 보여 주고 목록을 맞춘다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { acceptResult = ApiResult.Failure(ApiError.Server(409, "DRIVER_LINK_409_CLOSED", "만료되었거나 이미 처리된 요청입니다.")) }
        val vm = LinksViewModel(fake)
        advanceUntilIdle()
        vm.accept(invite)
        advanceUntilIdle()
        assertEquals("만료되었거나 이미 처리된 요청입니다.", vm.state.value.error)
        assertTrue(vm.state.value.invitations.isEmpty())
        assertNull(vm.state.value.notice)
        assertFalse(vm.state.value.isBusy)
    }

    @Test
    fun `거절하고 끊을 수 있다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = LinksViewModel(fake)
        advanceUntilIdle()
        vm.decline(invite)
        advanceUntilIdle()
        vm.unlink(linked)
        advanceUntilIdle()
        assertEquals(listOf("decline1", "unlink1"), fake.calls)
    }
}
