package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.auth.AuthRepository
import com.ieo.mobile.domain.auth.Consent
import com.ieo.mobile.domain.auth.ExistingAccount
import com.ieo.mobile.domain.auth.PhoneFormat
import com.ieo.mobile.domain.auth.PhoneVerification
import com.ieo.mobile.domain.vehicle.NewVehicle
import com.ieo.mobile.domain.vehicle.Tonnage
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.login.LoginStep
import com.ieo.mobile.ui.login.LoginViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private class FakeAuth(var existing: ExistingAccount? = null) : AuthRepository {
        var codeResult: ApiResult<String?> = ApiResult.Success(null)
        var verifyResult: ApiResult<PhoneVerification>? = null
        var signUpResult: ApiResult<Unit> = ApiResult.Success(Unit)
        var loggedInWith: String? = null
        var signedUp: Triple<String, Set<Consent>, NewVehicle?>? = null
        var finished = false
        override suspend fun requestCode(phone: String) = codeResult
        override suspend fun verify(phone: String, code: String) =
            verifyResult ?: ApiResult.Success(PhoneVerification("token", existing))
        override suspend fun login(phoneToken: String): ApiResult<Unit> {
            loggedInWith = phoneToken
            return ApiResult.Success(Unit)
        }
        override suspend fun signUp(phoneToken: String, name: String, consents: Set<Consent>, vehicle: NewVehicle?): ApiResult<Unit> {
            signedUp = Triple(name, consents, vehicle)
            return signUpResult
        }
        override fun finishSignUp() {
            finished = true
        }
    }

    private fun LoginViewModel.verifyPhone() {
        start()
        onPhoneChange("010-1234-5678")
        requestCode()
        main.dispatcher.scheduler.advanceUntilIdle()
        onCodeChange("123456")
        verify()
        main.dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `번호는 숫자만 남기고 형식이 맞아야 인증번호를 받는다`() {
        val vm = LoginViewModel(FakeAuth())
        vm.onPhoneChange("010-12")
        assertEquals("01012", vm.state.value.phone)
        assertFalse(vm.state.value.canRequestCode)
        vm.onPhoneChange("01012345678")
        assertTrue(vm.state.value.canRequestCode)
    }

    @Test
    fun `번호는 자리에 맞게 하이픈으로 보여 준다`() {
        assertEquals("010", PhoneFormat.hyphen("010"))
        assertEquals("010-1234", PhoneFormat.hyphen("0101234"))
        assertEquals("010-1234-5678", PhoneFormat.hyphen("01012345678"))
        assertEquals("011-123-4567", PhoneFormat.hyphen("0111234567"))
    }

    @Test
    fun `인증번호를 보내면 3분 유효 1분 뒤 재전송 시각을 정한다`() = runTest(main.dispatcher) {
        val vm = LoginViewModel(FakeAuth(), now = { 1_000L })
        vm.onPhoneChange("01012345678")
        vm.requestCode()
        advanceUntilIdle()
        assertEquals(LoginStep.CODE, vm.state.value.step)
        assertEquals(181_000L, vm.state.value.codeExpiresAtMillis)
        assertEquals(61_000L, vm.state.value.resendAtMillis)
    }

    @Test
    fun `틀린 인증번호는 칸을 빨갛게 하고 고치면 풀린다`() = runTest(main.dispatcher) {
        val auth = FakeAuth().apply {
            verifyResult = ApiResult.Failure(ApiError.Server(400, "AUTH_400_CODE", "인증번호가 맞지 않아요"))
        }
        val vm = LoginViewModel(auth)
        vm.verifyPhone()
        assertTrue(vm.state.value.codeRejected)
        assertEquals("인증번호가 맞지 않아요", vm.state.value.error)
        vm.onCodeChange("12345")
        assertFalse(vm.state.value.codeRejected)
    }

    @Test
    fun `시간이 지나면 확인을 막고 다시 받으면 풀린다`() = runTest(main.dispatcher) {
        val vm = LoginViewModel(FakeAuth())
        vm.onPhoneChange("01012345678")
        vm.requestCode()
        advanceUntilIdle()
        vm.onCodeTimeUp()
        vm.onCodeChange("123456")
        advanceUntilIdle()
        assertEquals(LoginStep.CODE, vm.state.value.step)
        assertFalse(vm.state.value.canVerify)
        vm.requestCode()
        advanceUntilIdle()
        assertFalse(vm.state.value.codeExpired)
        assertEquals("", vm.state.value.code)
    }

    @Test
    fun `6자리를 다 넣으면 자동으로 확인한다`() = runTest(main.dispatcher) {
        val vm = LoginViewModel(FakeAuth())
        vm.onPhoneChange("01012345678")
        vm.requestCode()
        advanceUntilIdle()
        vm.onCodeChange("12345")
        advanceUntilIdle()
        assertEquals(LoginStep.CODE, vm.state.value.step)
        vm.onCodeChange("123456")
        advanceUntilIdle()
        assertEquals(LoginStep.TERMS, vm.state.value.step)
    }

    @Test
    fun `있는 차주 계정이면 창을 띄우고 확인 뒤 로그인한다`() = runTest(main.dispatcher) {
        val auth = FakeAuth(ExistingAccount("김기사", isDriver = true))
        val vm = LoginViewModel(auth)
        vm.verifyPhone()
        assertEquals("김기사", vm.state.value.existing?.name)
        vm.login()
        advanceUntilIdle()
        assertEquals("token", auth.loggedInWith)
    }

    @Test
    fun `있는 계정 창에서 처음으로 가면 처음부터 시작한다`() = runTest(main.dispatcher) {
        val vm = LoginViewModel(FakeAuth(ExistingAccount("김기사", isDriver = true)))
        vm.verifyPhone()
        vm.restart()
        assertEquals(LoginStep.START, vm.state.value.step)
        assertNull(vm.state.value.existing)
        assertEquals("", vm.state.value.phone)
    }

    @Test
    fun `주선사 계정은 들어오지 못하게 안내한다`() = runTest(main.dispatcher) {
        val vm = LoginViewModel(FakeAuth(ExistingAccount("대표", isDriver = false)))
        vm.verifyPhone()
        assertEquals(LoginStep.PHONE, vm.state.value.step)
        assertEquals(LoginViewModel.BROKER_MESSAGE, vm.state.value.error)
    }

    @Test
    fun `필수 동의가 있어야 다음으로 간다`() = runTest(main.dispatcher) {
        val vm = LoginViewModel(FakeAuth())
        vm.verifyPhone()
        assertEquals(LoginStep.TERMS, vm.state.value.step)
        assertFalse(vm.state.value.canAgree)
        Consent.entries.filter { it.required }.forEach(vm::toggleConsent)
        assertTrue(vm.state.value.canAgree)
    }

    @Test
    fun `가입하면 완료 화면을 보여 주고 시작하기를 눌러야 로그인한다`() = runTest(main.dispatcher) {
        val auth = FakeAuth()
        val vm = LoginViewModel(auth)
        vm.verifyPhone()
        vm.agreeToTerms()
        vm.onNameChange(" 김기사 ")
        vm.onPlateChange("경기82사1234")
        vm.onTonnage(Tonnage.TON_11)
        vm.nameDone()
        assertEquals(LoginStep.VEHICLE, vm.state.value.step)
        vm.signUp()
        advanceUntilIdle()
        assertEquals("김기사", auth.signedUp?.first)
        assertEquals(Tonnage.TON_11, auth.signedUp?.third?.tonnage)
        assertEquals(LoginStep.DONE, vm.state.value.step)
        assertFalse(auth.finished)
        vm.finish()
        assertTrue(auth.finished)
    }

    @Test
    fun `나중에를 누르면 차량 없이 가입한다`() = runTest(main.dispatcher) {
        val auth = FakeAuth()
        val vm = LoginViewModel(auth)
        vm.verifyPhone()
        vm.agreeToTerms()
        vm.onNameChange("김기사")
        vm.nameDone()
        vm.onPlateChange("경기82사1234")
        vm.signUp(withVehicle = false)
        advanceUntilIdle()
        assertNull(auth.signedUp?.third)
    }

    @Test
    fun `가입이 실패하면 입력 화면에 남아 이유를 보여 준다`() = runTest(main.dispatcher) {
        val auth = FakeAuth().apply { signUpResult = ApiResult.Failure(ApiError.Server(400, "VEHICLE_400", "차량 번호 형식이 올바르지 않습니다")) }
        val vm = LoginViewModel(auth)
        vm.verifyPhone()
        vm.agreeToTerms()
        vm.onNameChange("김기사")
        vm.nameDone()
        vm.onPlateChange("ABC")
        vm.signUp()
        advanceUntilIdle()
        assertEquals(LoginStep.VEHICLE, vm.state.value.step)
        assertEquals("차량 번호 형식이 올바르지 않습니다", vm.state.value.error)
    }

    @Test
    fun `전체 동의는 모두 켜고 다시 누르면 모두 끈다`() {
        val vm = LoginViewModel(FakeAuth())
        vm.toggleAllConsents()
        assertEquals(Consent.entries.toSet(), vm.state.value.consents)
        vm.toggleAllConsents()
        assertTrue(vm.state.value.consents.isEmpty())
    }

    @Test
    fun `뒤로 가기는 한 단계씩 돌아가고 시작 화면에서 멈춘다`() {
        val vm = LoginViewModel(FakeAuth())
        vm.start()
        assertTrue(vm.back())
        assertEquals(LoginStep.START, vm.state.value.step)
        assertFalse(vm.back())
    }
}
