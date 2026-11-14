package com.ieo.mobile.domain.recommendation

import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.order.CargoCategory

/** 배차 추천 설정 (REC-004, 005). 차량 앱이 차를 세웠을 때 보여 주는 근처 추천 3건에 쓰인다. [minNetProfit]이 null이면 조건 없음 */
data class RecommendationPreference(
    val enabled: Boolean = true,
    val radiusKm: Int = DEFAULT_RADIUS_KM,
    val minNetProfit: Long? = null,
    val excludedCargo: Set<CargoCategory> = emptySet(),
    /** 지금 플랜으로 추천을 받을 수 있는지. 베이직이면 false (프로만, 2026-11-14) */
    val available: Boolean = true,
) {
    /** 내 정보에 보여 줄 한 줄 */
    val summary: String get() = when {
        !available -> "프로에서 받아요"
        enabled -> "켬 · ${radiusKm}km"
        else -> "꺼짐"
    }

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

/**
 * 오더 찾기의 "제외할 화물"과 배차 추천의 "제외할 화물"은 같은 설정이다 (DSP-010, REC-004).
 * 한쪽에서 바꾸면 다른 쪽에도 그대로 적용된다. 서버에는 추천 설정으로 저장한다
 */
interface ExcludedCargoStore {
    /** 저장된 제외할 화물. 못 받으면 null */
    suspend fun load(): Set<CargoCategory>?

    suspend fun save(excluded: Set<CargoCategory>)

    /** 저장소 없이 쓸 때 (화면 미리보기·테스트) */
    object None : ExcludedCargoStore {
        override suspend fun load(): Set<CargoCategory>? = null
        override suspend fun save(excluded: Set<CargoCategory>) = Unit
    }
}

/** 추천 설정을 읽어 제외할 화물만 바꿔 저장한다 */
class PreferenceExcludedCargoStore(private val repository: RecommendationRepository) : ExcludedCargoStore {
    override suspend fun load(): Set<CargoCategory>? = (repository.preference() as? ApiResult.Success)?.data?.excludedCargo

    override suspend fun save(excluded: Set<CargoCategory>) {
        val current = (repository.preference() as? ApiResult.Success)?.data ?: return
        if (current.excludedCargo != excluded) repository.save(current.copy(excludedCargo = excluded))
    }
}

interface RecommendationRepository {

    suspend fun preference(): ApiResult<RecommendationPreference>

    suspend fun save(preference: RecommendationPreference): ApiResult<RecommendationPreference>
}
