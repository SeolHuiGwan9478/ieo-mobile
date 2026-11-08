package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.carlogin.CarLoginRepository
import com.ieo.mobile.domain.carlogin.CarLoginRequest
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.carlogin.CarLoginState
import com.ieo.mobile.ui.carlogin.CarLoginViewModel
import java.time.Instant
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CarLoginViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private class Fake(val pending: Boolean = true) : CarLoginRepository {
        var approveResult: ApiResult<Unit> = ApiResult.Success(Unit)
        val answers = mutableListOf<String>()
        override suspend fun find(code: String) =
            ApiResult.Success(CarLoginRequest(code, "내 차", null, Instant.EPOCH, pending))
        override suspend fun approve(code: String): ApiResult<Unit> {
            answers += "approve"
            return approveResult
        }
        override suspend fun reject(code: String): ApiResult<Unit> {
            answers += "reject"
            return ApiResult.Success(Unit)
        }
    }

    @Test
    fun `승인하면 서버에 보내고 끝난다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = CarLoginViewModel(fake).apply { load("KX7P-M4QZ") }
        advanceUntilIdle()
        vm.approve()
        advanceUntilIdle()
        assertEquals(listOf("approve"), fake.answers)
        assertEquals(CarLoginState.Approved, vm.state.value)
    }

    @Test
    fun `이미 처리된 요청은 승인할 수 없다`() = runTest(main.dispatcher) {
        val fake = Fake(pending = false)
        val vm = CarLoginViewModel(fake).apply { load("KX7P-M4QZ") }
        advanceUntilIdle()
        assertEquals(CarLoginState.Failed(CarLoginViewModel.NOT_PENDING_MESSAGE), vm.state.value)
        vm.approve()
        advanceUntilIdle()
        assertTrue(fake.answers.isEmpty())
    }

    @Test
    fun `승인이 실패하면 화면에 남아 다시 누를 수 있다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { approveResult = ApiResult.Failure(ApiError.Network) }
        val vm = CarLoginViewModel(fake).apply { load("KX7P-M4QZ") }
        advanceUntilIdle()
        vm.approve()
        advanceUntilIdle()
        val state = vm.state.value as CarLoginState.Asking
        assertEquals(ApiError.Network.message, state.error)
        assertEquals(false, state.isBusy)
    }
}
