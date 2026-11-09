package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.account.AccountSettingsRepository
import com.ieo.mobile.domain.account.BusinessInfo
import com.ieo.mobile.domain.account.CarSession
import com.ieo.mobile.domain.account.ConsentItem
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.my.AccountSettingsViewModel
import java.time.Instant
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountSettingsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val car = CarSession("a", "내 트럭", Instant.parse("2026-11-08T03:00:00Z"))
    private val marketing = ConsentItem("MARKETING", "마케팅", false, false)
    private val terms = ConsentItem("TERMS", "이용약관", true, true)

    private inner class Fake : AccountSettingsRepository {
        var saved: BusinessInfo? = null
        var consentResult: ApiResult<ConsentItem>? = null
        var saveFails = false
        val calls = mutableListOf<String>()
        override suspend fun carSessions() = ApiResult.Success(listOf(car))
        override suspend fun revokeCarSession(id: String): ApiResult<Unit> {
            calls += "revoke$id"
            return ApiResult.Success(Unit)
        }
        override suspend fun consents() = ApiResult.Success(listOf(terms, marketing))
        override suspend fun changeConsent(type: String, agreed: Boolean): ApiResult<ConsentItem> {
            calls += "$type:$agreed"
            return consentResult ?: ApiResult.Success(marketing.copy(agreed = agreed))
        }
        override suspend fun business(): ApiResult<BusinessInfo?> = ApiResult.Success(saved)
        override suspend fun saveBusiness(info: BusinessInfo): ApiResult<BusinessInfo?> {
            if (saveFails) return ApiResult.Failure(ApiError.Network)
            saved = info
            return ApiResult.Success(info)
        }
        override suspend fun clearBusiness(): ApiResult<Unit> {
            saved = null
            return ApiResult.Success(Unit)
        }
    }

    @Test
    fun `차량을 로그아웃하면 목록에서 빠진다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = AccountSettingsViewModel(fake)
        advanceUntilIdle()
        vm.revoke(car)
        advanceUntilIdle()
        assertEquals(listOf("revokea"), fake.calls)
        assertTrue(vm.state.value.carSessions.isEmpty())
        assertEquals("내 트럭에서 로그아웃했어요", vm.state.value.notice)
    }

    @Test
    fun `선택 동의를 바꾸고 실패하면 되돌린다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = AccountSettingsViewModel(fake)
        advanceUntilIdle()
        vm.setConsent(marketing, true)
        advanceUntilIdle()
        assertTrue(vm.state.value.consents.first { it.type == "MARKETING" }.agreed)

        fake.consentResult = ApiResult.Failure(ApiError.Network)
        vm.setConsent(marketing.copy(agreed = true), false)
        advanceUntilIdle()
        assertTrue(vm.state.value.consents.first { it.type == "MARKETING" }.agreed)
        assertTrue(vm.state.value.error != null)
    }

    @Test
    fun `필수 동의는 바꾸지 않는다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = AccountSettingsViewModel(fake)
        advanceUntilIdle()
        vm.setConsent(terms, false)
        advanceUntilIdle()
        assertTrue(fake.calls.isEmpty())
    }

    @Test
    fun `사업자등록번호는 숫자 10자리여야 저장되고 지우면 비워진다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = AccountSettingsViewModel(fake)
        advanceUntilIdle()
        vm.onBusinessName("한길운수")
        vm.onRepresentative("홍길동")
        vm.onBusinessNumber("123-45")
        assertFalse(vm.state.value.form.canSave)
        vm.onBusinessNumber("123-45-678911")
        assertEquals("1234567891", vm.state.value.form.number)
        vm.saveBusiness()
        advanceUntilIdle()
        assertEquals("한길운수", fake.saved?.name)
        vm.clearBusiness()
        advanceUntilIdle()
        assertNull(vm.state.value.saved)
        assertEquals("", vm.state.value.form.name)
    }
}
