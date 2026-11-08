package com.ieo.mobile.domain.site

import com.ieo.mobile.core.network.ApiResult
import java.time.Instant

/** 내 차 높이와 높이 제한 비교 (VEH-002) */
enum class HeightLevel { BLOCKED, CAUTION, OK, UNKNOWN }

data class HeightCheck(val level: HeightLevel, val vehicleM: Double?, val limitM: Double?)

/** 아무도 남기지 않은 항목은 null */
data class SiteAttributes(
    val heightLimitM: Double? = null,
    val forklift: Boolean? = null,
    val parking: Boolean? = null,
    val updatedAt: Instant? = null,
)

/** 기록이 3건 미만이면 서버가 주지 않는다 */
data class SiteStats(val avgWaitMinutes: Int?, val avgWorkMinutes: Int?, val sampleCount: Int)

/** 작성자는 익명이고 내가 쓴 것인지만 안다 (SITE-008) */
data class SiteNote(val id: Long, val text: String, val helpfulCount: Int, val mine: Boolean, val createdAt: Instant)

data class SiteCard(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val attributes: SiteAttributes,
    val height: HeightCheck,
    val stats: SiteStats?,
    val notes: List<SiteNote>,
)

enum class SiteAttributeKey { HEIGHT_LIMIT, FORKLIFT, PARKING }

/** 높이 제한 입력을 서버 범위(1.5~6m, 소수 둘째 자리까지)로 확인한다 */
object SiteHeight {
    const val MIN_M = 1.5
    const val MAX_M = 6.0

    /** 맞으면 서버에 보낼 값, 아니면 null */
    fun normalize(input: String): String? {
        val text = input.trim().removeSuffix("m").trim()
        if (!Regex("""\d(\.\d{1,2})?""").matches(text)) return null
        return text.takeIf { it.toDouble() in MIN_M..MAX_M }
    }
}

interface SiteRepository {

    suspend fun card(siteId: Long): ApiResult<SiteCard>

    suspend fun addAttribute(siteId: Long, key: SiteAttributeKey, value: String): ApiResult<SiteAttributes>

    suspend fun addNote(siteId: Long, text: String): ApiResult<SiteNote>
}
