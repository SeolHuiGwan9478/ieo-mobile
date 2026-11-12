package com.ieo.mobile.domain.proof

import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.trip.StopType
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** 상하차지 대기 기록 (02 대기). 기록 자체는 차량 앱이 남긴다. 분과 금액은 아직 정해지지 않았으면 null */
data class WaitRecord(
    val stopType: StopType,
    val siteName: String,
    val arrivedAt: Instant,
    val workStartedAt: Instant?,
    val departedAt: Instant?,
    val waitMinutes: Long?,
    val chargeableAmount: Long?,
    val hasPolicy: Boolean,
    val edited: Boolean,
) {
    val isWaiting: Boolean get() = workStartedAt == null && departedAt == null

    /** 작업 시작 없이 떠나면 대기와 작업을 나눌 수 없어 대기비를 계산하지 않는다 (02 대기 구간 정의) */
    val isUndivided: Boolean get() = workStartedAt == null && departedAt != null
}

data class ProofLink(val url: String, val expiresAt: Instant)

/** 인수증 상태 (04 인수증). 사진이 없으면 NONE, 올렸지만 안 보냈으면 PENDING, 주선사에게 보냈으면 SENT */
enum class DeliveryProofStatus { NONE, PENDING, SENT }

data class ReceiptPhoto(val id: Long)

/**
 * 종이 인수증 사진. 결제 방식이 인수증인 운행만 [required].
 * [canUpload]는 서버가 정한다: 하차지 도착 뒤, 아직 안 보냄, [maxPhotos]장 미만
 */
data class DeliveryProof(
    val required: Boolean,
    val status: DeliveryProofStatus,
    val sentAt: Instant?,
    val photos: List<ReceiptPhoto>,
    val maxPhotos: Int,
    val canUpload: Boolean,
) {
    val isSent: Boolean get() = status == DeliveryProofStatus.SENT
    val canSend: Boolean get() = !isSent && photos.isNotEmpty()
}

interface ProofRepository {

    suspend fun waits(tripId: Long): ApiResult<List<WaitRecord>>

    /** 운행의 대기 증빙 링크 (상차지·하차지 함께, WAIT-020). 이미 만든 링크가 있으면 같은 링크 */
    suspend fun waitProofLink(tripId: Long): ApiResult<ProofLink>

    /** 대기 시각 고치기 (WAIT-006). 고친 기록에는 "수정됨"과 원래 값이 남는다 */
    suspend fun editWait(tripId: Long, stop: StopType, times: WaitTimes): ApiResult<WaitRecord>

    suspend fun deliveryProof(tripId: Long): ApiResult<DeliveryProof>

    suspend fun uploadReceiptPhoto(tripId: Long, jpeg: ByteArray): ApiResult<DeliveryProof>

    /** 보내기 전에만 지울 수 있다 */
    suspend fun deleteReceiptPhoto(tripId: Long, photoId: Long): ApiResult<DeliveryProof>

    /** [주선사에게 보내기]. 보낸 뒤에는 사진을 바꿀 수 없다 */
    suspend fun sendReceipt(tripId: Long): ApiResult<DeliveryProof>

    /** 사진 파일 (로그인 토큰이 있어야 받는다) */
    suspend fun receiptPhoto(tripId: Long, photoId: Long): ApiResult<ByteArray>
}

/** 고칠 대기 시각. 작업 시작·출발은 비울 수 있다 */
data class WaitTimes(val arrivedAt: Instant, val workStartedAt: Instant?, val departedAt: Instant?)

/**
 * "14:05" 같은 시각을 도착한 날 기준으로 바꾼다 (WAIT-006). 앞 시각보다 이르면 다음 날로 본다(밤샘 대기).
 * 비운 칸은 null. 형식이 틀리거나 지금보다 늦으면 실패 문구를 준다
 */
object WaitTimeInput {
    private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    private val PATTERN = Regex("""^([01]?\d|2[0-3]):([0-5]\d)$""")

    fun resolve(base: Instant, arrived: String, work: String, depart: String, now: Instant): Result<WaitTimes> {
        val day = base.atZone(ZONE).toLocalDate()
        var previous: Instant? = null
        val resolved = mutableListOf<Instant?>()
        for ((label, text) in listOf("도착" to arrived, "작업 시작" to work, "출발" to depart)) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                if (label == "도착") return Result.failure(IllegalArgumentException("도착 시각을 넣어 주세요"))
                resolved += null
                continue
            }
            val match = PATTERN.matchEntire(trimmed) ?: return Result.failure(IllegalArgumentException("$label 시각은 14:05처럼 넣어 주세요"))
            var at = day.atTime(match.groupValues[1].toInt(), match.groupValues[2].toInt()).atZone(ZONE).toInstant()
            if (previous != null && at.isBefore(previous)) at = at.plus(Duration.ofDays(1))
            if (at.isAfter(now)) return Result.failure(IllegalArgumentException("$label 시각이 지금보다 늦어요"))
            resolved += at
            previous = at
        }
        return Result.success(WaitTimes(resolved[0]!!, resolved[1], resolved[2]))
    }
}
