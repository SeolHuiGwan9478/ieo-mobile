package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.account.Account
import com.ieo.mobile.domain.account.AccountRepository
import com.ieo.mobile.domain.auth.AuthRepository
import com.ieo.mobile.domain.auth.Consent
import com.ieo.mobile.domain.auth.ExistingAccount
import com.ieo.mobile.domain.auth.PhoneVerification
import com.ieo.mobile.domain.vehicle.NewVehicle
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.my.MyViewModel
import com.ieo.mobile.ui.my.ProfileEditViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MyViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val me = Account("김기사", "01012345678")

    private inner class Fake : AccountRepository {
        var withdrawResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var renamedTo: String? = null
        var phoneToken: String? = null
        override suspend fun me() = ApiResult.Success(me)
        override suspend fun rename(name: String): ApiResult<Account> {
            renamedTo = name
            return ApiResult.Success(me.copy(name = name))
        }
        override suspend fun changePhone(phoneToken: String): ApiResult<Account> {
            this.phoneToken = phoneToken
            return ApiResult.Success(me.copy(phone = "01099998888"))
        }
        override suspend fun withdraw() = withdrawResult
        override fun signOut() = Unit
    }

    private class FakeAuth(val existing: ExistingAccount? = null) : AuthRepository {
        override suspend fun requestCode(phone: String) = ApiResult.Success<String?>("123456")
        override suspend fun verify(phone: String, code: String) = ApiResult.Success(PhoneVerification("token-$phone", existing))
        override suspend fun login(phoneToken: String) = ApiResult.Success(Unit)
        override suspend fun signUp(phoneToken: String, name: String, consents: Set<Consent>, vehicle: NewVehicle?) = ApiResult.Success(Unit)
        override fun finishSignUp() = Unit
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

    @Test
    fun `이름만 바꾸면 인증 없이 저장한다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = ProfileEditViewModel(fake, FakeAuth())
        vm.start(me)
        assertFalse(vm.state.value.canSave)
        vm.onName(" 새이름 ")
        assertTrue(vm.state.value.canSave)
        var saved: Account? = null
        vm.save { saved = it }
        advanceUntilIdle()
        assertEquals("새이름", fake.renamedTo)
        assertEquals("새이름", saved?.name)
        assertNull(fake.phoneToken)
    }

    @Test
    fun `번호를 바꾸면 새 번호로 인증해야 저장할 수 있다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = ProfileEditViewModel(fake, FakeAuth())
        vm.start(me)
        vm.onPhone("010-9999-8888")
        assertFalse(vm.state.value.canSave)
        vm.sendCode()
        advanceUntilIdle()
        assertEquals("123456", vm.state.value.demoCode)
        vm.onCode("123456")
        vm.verify()
        advanceUntilIdle()
        assertTrue(vm.state.value.phoneVerified)
        var saved: Account? = null
        vm.save { saved = it }
        advanceUntilIdle()
        assertEquals("token-01099998888", fake.phoneToken)
        assertEquals("01099998888", saved?.phone)
    }

    @Test
    fun `다른 계정이 쓰는 번호면 확인에서 막는다`() = runTest(main.dispatcher) {
        val vm = ProfileEditViewModel(Fake(), FakeAuth(existing = ExistingAccount("이주선", isDriver = false)))
        vm.start(me)
        vm.onPhone("01077776666")
        vm.sendCode()
        advanceUntilIdle()
        vm.onCode("123456")
        vm.verify()
        advanceUntilIdle()
        assertFalse(vm.state.value.phoneVerified)
        assertEquals(ProfileEditViewModel.TAKEN_MESSAGE, vm.state.value.error)
    }

    @Test
    fun `번호를 다시 고치면 인증을 처음부터 한다`() = runTest(main.dispatcher) {
        val vm = ProfileEditViewModel(Fake(), FakeAuth())
        vm.start(me)
        vm.onPhone("01099998888")
        vm.sendCode()
        advanceUntilIdle()
        vm.onCode("123456")
        vm.verify()
        advanceUntilIdle()
        vm.onPhone("01099998887")
        assertFalse(vm.state.value.phoneVerified)
        assertFalse(vm.state.value.codeSent)
    }
}
