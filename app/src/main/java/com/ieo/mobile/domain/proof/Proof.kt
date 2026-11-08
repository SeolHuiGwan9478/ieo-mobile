package com.ieo.mobile.domain.proof

import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.trip.StopType
import java.time.Instant

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

enum class DeliveryProofStatus { PENDING, SIGNED, PHOTO, DELIVERED }

data class DeliveryProof(
    val url: String,
    val status: DeliveryProofStatus,
    val signerName: String?,
    val signedAt: Instant?,
    val issueNote: String?,
    val photoCount: Int,
    val expiresAt: Instant?,
) {
    val isSigned: Boolean get() = signedAt != null
    val isDone: Boolean get() = status != DeliveryProofStatus.PENDING
    val canAddPhoto: Boolean get() = !isSigned && photoCount < MAX_PHOTOS

    companion object {
        const val MAX_PHOTOS = 5
    }
}

interface ProofRepository {

    suspend fun waits(tripId: Long): ApiResult<List<WaitRecord>>

    /** 이미 만든 링크가 있으면 같은 링크 */
    suspend fun waitProofLink(tripId: Long, stop: StopType): ApiResult<ProofLink>

    /** 아직 발급하지 않았으면 null */
    suspend fun deliveryProof(tripId: Long): ApiResult<DeliveryProof?>

    /** 이미 있으면 같은 인수증 */
    suspend fun issueDeliveryProof(tripId: Long): ApiResult<DeliveryProof>

    suspend fun uploadPaperPhoto(tripId: Long, jpeg: ByteArray): ApiResult<DeliveryProof>
}
