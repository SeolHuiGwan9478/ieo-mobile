package com.ieo.mobile.data.site

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.site.HeightCheck
import com.ieo.mobile.domain.site.HeightLevel
import com.ieo.mobile.domain.site.SiteAttributeKey
import com.ieo.mobile.domain.site.SiteAttributes
import com.ieo.mobile.domain.site.SiteCard
import com.ieo.mobile.domain.site.SiteNote
import com.ieo.mobile.domain.site.SiteRepository
import com.ieo.mobile.domain.site.SiteStats
import java.time.Instant
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SiteApi {

    @GET("api/sites/{id}")
    suspend fun card(@Path("id") id: Long): Response<SiteCardResponse>

    @POST("api/sites/{id}/attributes")
    suspend fun addAttribute(@Path("id") id: Long, @Body body: SiteAttributeRequest): Response<SiteAttributesResponse>

    @POST("api/sites/{id}/notes")
    suspend fun addNote(@Path("id") id: Long, @Body body: SiteNoteRequest): Response<SiteNoteResponse>
}

@Serializable
data class SiteAttributeRequest(val key: String, val value: String)

@Serializable
data class SiteNoteRequest(val text: String)

@Serializable
data class SiteAttributesResponse(
    val heightLimitM: Double? = null,
    val forklift: Boolean? = null,
    val parking: Boolean? = null,
    val updatedAt: String? = null,
) {
    fun toDomain() = SiteAttributes(heightLimitM, forklift, parking, updatedAt?.let(Instant::parse))
}

@Serializable
data class HeightWarningResponse(val level: String, val vehicleHeightM: Double? = null, val limitM: Double? = null) {
    fun toDomain() = HeightCheck(
        HeightLevel.entries.firstOrNull { it.name == level } ?: HeightLevel.UNKNOWN,
        vehicleHeightM,
        limitM,
    )
}

@Serializable
data class SiteStatsResponse(val avgWaitMinutes: Int? = null, val avgWorkMinutes: Int? = null, val sampleCount: Int) {
    fun toDomain() = SiteStats(avgWaitMinutes, avgWorkMinutes, sampleCount)
}

@Serializable
data class SiteNoteResponse(val id: Long, val text: String, val helpfulCount: Int, val mine: Boolean, val createdAt: String) {
    fun toDomain() = SiteNote(id, text, helpfulCount, mine, Instant.parse(createdAt))
}

@Serializable
data class SiteCardResponse(
    val id: Long,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val attributes: SiteAttributesResponse,
    val heightWarning: HeightWarningResponse,
    val stats: SiteStatsResponse? = null,
    val notes: List<SiteNoteResponse>,
) {
    fun toDomain() = SiteCard(id, name, address, lat, lng, attributes.toDomain(), heightWarning.toDomain(), stats?.toDomain(), notes.map { it.toDomain() })
}

class DefaultSiteRepository(private val api: SiteApi, private val caller: ApiCaller) : SiteRepository {

    override suspend fun card(siteId: Long): ApiResult<SiteCard> =
        caller.call { api.card(siteId) }.mapResponse { it.toDomain() }

    override suspend fun addAttribute(siteId: Long, key: SiteAttributeKey, value: String): ApiResult<SiteAttributes> =
        caller.call { api.addAttribute(siteId, SiteAttributeRequest(key.name, value)) }.mapResponse { it.toDomain() }

    override suspend fun addNote(siteId: Long, text: String): ApiResult<SiteNote> =
        caller.call { api.addNote(siteId, SiteNoteRequest(text)) }.mapResponse { it.toDomain() }
}
