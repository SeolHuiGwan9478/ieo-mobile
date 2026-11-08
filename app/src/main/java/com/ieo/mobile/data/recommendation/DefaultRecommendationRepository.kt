package com.ieo.mobile.data.recommendation

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.order.CargoCategory
import com.ieo.mobile.domain.recommendation.RecommendationPreference
import com.ieo.mobile.domain.recommendation.RecommendationRepository
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface RecommendationApi {

    @GET("api/recommendation-preference")
    suspend fun preference(): Response<RecommendationPreferenceBody>

    @PUT("api/recommendation-preference")
    suspend fun save(@Body body: RecommendationPreferenceBody): Response<RecommendationPreferenceBody>
}

/** 제외 화물의 기본값은 null이다: 빈 목록도 보내야 서버가 "모두 받기"로 바꾼다 */
@Serializable
data class RecommendationPreferenceBody(
    val enabled: Boolean,
    val radiusKm: Int,
    val minNetProfit: Long? = null,
    val excludedCargoCategories: List<String>? = null,
) {
    fun toDomain() = RecommendationPreference(
        enabled,
        radiusKm,
        minNetProfit,
        excludedCargoCategories.orEmpty().mapNotNull(CargoCategory::fromName).toSet(),
    )

    companion object {
        fun of(preference: RecommendationPreference) = RecommendationPreferenceBody(
            preference.enabled,
            preference.radiusKm,
            preference.minNetProfit,
            preference.excludedCargo.sortedBy { it.ordinal }.map { it.name },
        )
    }
}

class DefaultRecommendationRepository(
    private val api: RecommendationApi,
    private val caller: ApiCaller,
) : RecommendationRepository {

    override suspend fun preference(): ApiResult<RecommendationPreference> =
        caller.call { api.preference() }.mapResponse { it.toDomain() }

    override suspend fun save(preference: RecommendationPreference): ApiResult<RecommendationPreference> =
        caller.call { api.save(RecommendationPreferenceBody.of(preference)) }.mapResponse { it.toDomain() }
}
