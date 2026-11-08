package com.ieo.mobile.domain.recommendation

import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.order.CargoCategory

/** 다음 배차 추천 설정 (REC-004, 025). 차량 앱의 음성·수락 창에 쓰인다. [minNetProfit]이 null이면 조건 없음 */
data class RecommendationPreference(
    val enabled: Boolean = true,
    val radiusKm: Int = DEFAULT_RADIUS_KM,
    val minNetProfit: Long? = null,
    val excludedCargo: Set<CargoCategory> = emptySet(),
) {
    /** 내 정보에 보여 줄 한 줄 */
    val summary: String get() = if (enabled) "켬 · ${radiusKm}km" else "꺼짐"

    companion object {
        const val DEFAULT_RADIUS_KM = 30
        const val MIN_RADIUS_KM = 5
        const val MAX_RADIUS_KM = 100
        const val RADIUS_STEP_KM = 5

        /** 5km 단위로 맞추고 범위 안으로 자른다 */
        fun stepRadius(radiusKm: Int, delta: Int): Int =
            ((radiusKm + delta) / RADIUS_STEP_KM * RADIUS_STEP_KM).coerceIn(MIN_RADIUS_KM, MAX_RADIUS_KM)
    }
}

interface RecommendationRepository {

    suspend fun preference(): ApiResult<RecommendationPreference>

    suspend fun save(preference: RecommendationPreference): ApiResult<RecommendationPreference>
}
