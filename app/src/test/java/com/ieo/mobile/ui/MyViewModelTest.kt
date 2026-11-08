package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.account.Account
import com.ieo.mobile.domain.account.AccountRepository
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.my.MyViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MyViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private class Fake : AccountRepository {
        var renameResult: ApiResult<Account> = ApiResult.Success(Account("새이름", "01012345678"))
        var withdrawResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var renamedTo: String? = null
        var signedOut = false
        override suspend fun me() = ApiResult.Success(Account("김기사", "01012345678"))
        override suspend fun rename(name: String): ApiResult<Account> {
            renamedTo = name
            return renameResult
        }
        override suspend fun withdraw() = withdrawResult
        override fun signOut() {
            signedOut = true
        }
    }

    @Test
    fun `이름을 바꾸면 화면에 반영하고 창을 닫는다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = MyViewModel(fake)
        advanceUntilIdle()
        var closed = false
        vm.rename(" 새이름 ") { closed = true }
        advanceUntilIdle()
        assertEquals("새이름", fake.renamedTo)
        assertEquals("새이름", vm.state.value.account?.name)
        assertTrue(closed)
    }

    @Test
    fun `이름 바꾸기가 실패하면 창을 닫지 않고 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { renameResult = ApiResult.Failure(ApiError.Network) }
        val vm = MyViewModel(fake)
        advanceUntilIdle()
        var closed = false
        vm.rename("새이름") { closed = true }
        advanceUntilIdle()
        assertFalse(closed)
        assertEquals(ApiError.Network.message, vm.state.value.message)
        assertEquals("김기사", vm.state.value.account?.name)
    }

    @Test
    fun `탈퇴가 실패하면 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { withdrawResult = ApiResult.Failure(ApiError.Network) }
        val vm = MyViewModel(fake)
        advanceUntilIdle()
        vm.withdraw()
        advanceUntilIdle()
        assertEquals(ApiError.Network.message, vm.state.value.message)
    }
}
