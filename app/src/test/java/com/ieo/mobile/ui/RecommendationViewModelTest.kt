package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.data.recommendation.RecommendationPreferenceBody
import com.ieo.mobile.domain.order.CargoCategory
import com.ieo.mobile.domain.recommendation.RecommendationPreference
import com.ieo.mobile.domain.recommendation.RecommendationRepository
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.recommendation.RecommendationViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecommendationViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private class Fake : RecommendationRepository {
        var result: ApiResult<RecommendationPreference>? = null
        var saved: RecommendationPreference? = null
        override suspend fun preference() = ApiResult.Success(RecommendationPreference())
        override suspend fun save(preference: RecommendationPreference): ApiResult<RecommendationPreference> {
            saved = preference
            return result ?: ApiResult.Success(preference)
        }
    }

    @Test
    fun `반경은 5km 단위로 범위 안에서 움직인다`() {
        assertEquals(35, RecommendationPreference.stepRadius(30, 5))
        assertEquals(5, RecommendationPreference.stepRadius(5, -5))
        assertEquals(100, RecommendationPreference.stepRadius(100, 5))
    }

    @Test
    fun `고친 게 있을 때만 저장할 수 있고 저장하면 끝난다`() = runTest(main.dispatcher) {
        val fake = Fake()
        val vm = RecommendationViewModel(fake)
        advanceUntilIdle()
        assertFalse(vm.state.value.hasChanges)
        vm.stepRadius(5)
        vm.toggleCargo(CargoCategory.COLD)
        assertTrue(vm.state.value.hasChanges)
        vm.save()
        advanceUntilIdle()
        assertEquals(35, fake.saved?.radiusKm)
        assertFalse(vm.state.value.hasChanges)
        assertTrue(vm.state.value.justSaved)
    }

    @Test
    fun `되돌리면 저장할 것이 없다`() = runTest(main.dispatcher) {
        val vm = RecommendationViewModel(Fake())
        advanceUntilIdle()
        vm.setEnabled(false)
        vm.discard()
        assertFalse(vm.state.value.hasChanges)
    }

    @Test
    fun `저장이 실패하면 고친 값을 남기고 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake().apply { result = ApiResult.Failure(ApiError.Network) }
        val vm = RecommendationViewModel(fake)
        advanceUntilIdle()
        vm.setMinNetProfit(200_000)
        vm.save()
        advanceUntilIdle()
        assertEquals(ApiError.Network.message, vm.state.value.error)
        assertTrue(vm.state.value.hasChanges)
    }

    @Test
    fun `빼는 화물을 다 지워도 빈 목록을 보낸다`() {
        assertEquals(emptyList<String>(), RecommendationPreferenceBody.of(RecommendationPreference()).excludedCargoCategories)
    }
}
