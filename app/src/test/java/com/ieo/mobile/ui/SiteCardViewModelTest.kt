package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.site.HeightCheck
import com.ieo.mobile.domain.site.HeightLevel
import com.ieo.mobile.domain.site.SiteAttributeKey
import com.ieo.mobile.domain.site.SiteAttributes
import com.ieo.mobile.domain.site.SiteCard
import com.ieo.mobile.domain.site.SiteHeight
import com.ieo.mobile.domain.site.SiteNote
import com.ieo.mobile.domain.site.SiteRepository
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.site.SiteCardViewModel
import com.ieo.mobile.ui.site.SiteMode
import java.time.Instant
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SiteCardViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private class Fake(val attributes: SiteAttributes = SiteAttributes(3.5, true, null)) : SiteRepository {
        val sent = mutableListOf<Pair<SiteAttributeKey, String>>()
        var failKey: SiteAttributeKey? = null
        var cardCalls = 0
        override suspend fun card(siteId: Long): ApiResult<SiteCard> {
            cardCalls++
            return ApiResult.Success(SiteCard(siteId, "평택", "경기", 36.0, 126.0, attributes, HeightCheck(HeightLevel.OK, 3.0, 3.5), null, emptyList()))
        }
        override suspend fun addAttribute(siteId: Long, key: SiteAttributeKey, value: String): ApiResult<SiteAttributes> {
            if (key == failKey) return ApiResult.Failure(ApiError.Network)
            sent += key to value
            return ApiResult.Success(attributes)
        }
        override suspend fun addNote(siteId: Long, text: String) = ApiResult.Success(SiteNote(1, text, 0, true, Instant.EPOCH))
    }

    private suspend fun kotlinx.coroutines.test.TestScope.opened(fake: Fake) = SiteCardViewModel(fake).also {
        it.open(1)
        advanceUntilIdle()
    }

    @Test
    fun `높이 입력은 서버 범위만 통과한다`() {
        assertEquals("3.5", SiteHeight.normalize(" 3.5m "))
        assertEquals("6", SiteHeight.normalize("6"))
        assertNull(SiteHeight.normalize("6.5"))
        assertNull(SiteHeight.normalize("1.2"))
        assertNull(SiteHeight.normalize("3.555"))
        assertNull(SiteHeight.normalize("abc"))
    }

    @Test
    fun `바뀐 항목만 보낸다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = opened(fake)
        vm.startEdit()
        vm.setHeight("3.8")
        vm.setParking(true)
        vm.saveInfo()
        advanceUntilIdle()
        assertEquals(listOf(SiteAttributeKey.HEIGHT_LIMIT to "3.8", SiteAttributeKey.PARKING to "true"), fake.sent)
        assertEquals(SiteMode.VIEW, vm.state.value.mode)
        assertNotNull(vm.state.value.saved)
        assertEquals(2, fake.cardCalls)
    }

    @Test
    fun `아무것도 안 바꾸면 보내지 않고 돌아간다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = opened(fake)
        vm.startEdit()
        vm.saveInfo()
        advanceUntilIdle()
        assertTrue(fake.sent.isEmpty())
        assertEquals(SiteMode.VIEW, vm.state.value.mode)
    }

    @Test
    fun `높이가 틀리면 보내지 않고 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = opened(fake)
        vm.startEdit()
        vm.setHeight("9")
        vm.saveInfo()
        advanceUntilIdle()
        assertTrue(fake.sent.isEmpty())
        assertEquals(SiteMode.EDIT_INFO, vm.state.value.mode)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `중간에 실패하면 입력을 남기고 카드를 새로 받는다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { failKey = SiteAttributeKey.PARKING }
        val vm = opened(fake)
        vm.startEdit()
        vm.setHeight("3.8")
        vm.setParking(true)
        vm.saveInfo()
        advanceUntilIdle()
        assertEquals(SiteMode.EDIT_INFO, vm.state.value.mode)
        assertEquals("3.8", vm.state.value.draft.height)
        assertEquals(ApiError.Network.message, vm.state.value.error)
        assertEquals(2, fake.cardCalls)
    }

    @Test
    fun `메모는 비면 못 남기고 남기면 맨 위에 보인다`() = runTest(main.dispatcher) {
        val vm = opened(Fake())
        vm.startNote()
        vm.setNote("   ")
        assertFalse(vm.state.value.canSaveNote)
        vm.setNote(" 후문으로 ")
        vm.saveNote()
        advanceUntilIdle()
        assertEquals("후문으로", vm.state.value.card?.notes?.first()?.text)
        assertEquals(SiteMode.VIEW, vm.state.value.mode)
    }
}
