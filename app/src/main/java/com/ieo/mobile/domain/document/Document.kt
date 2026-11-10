package com.ieo.mobile.domain.document

import com.ieo.mobile.core.network.ApiResult
import java.time.Instant

/** 서류 한 건의 심사 상태 (서버 DocumentStatus) */
enum class DocumentStatus { PENDING, APPROVED, REJECTED }

/** 서류 한 건. [rejectMessage]는 반려됐을 때 차주에게 그대로 보이는 문구 */
data class DocumentSubmission(
    val id: Long,
    val status: DocumentStatus,
    val submittedAt: Instant?,
    val rejectMessage: String?,
)

/**
 * 서류 종류 하나의 현재 상태. [latest]가 없으면 아직 낸 적 없다.
 * [checks]는 운영자가 사진에서 보는 항목이라 찍기 전에 보여 준다
 */
data class DocumentItem(
    val type: String,
    val label: String,
    val required: Boolean,
    val checks: List<String>,
    val latest: DocumentSubmission?,
) {
    val status: DocumentStatus? get() = latest?.status

    /** 지금 올릴 수 있다. 심사 중이면 결과가 나온 뒤에 다시 낸다 */
    val canSubmit: Boolean get() = status != DocumentStatus.PENDING

    /** 사업자등록증은 개업일(과 사업자번호)을 같이 보내 국세청 진위확인을 돌린다 */
    val needsBusinessInfo: Boolean get() = type == BUSINESS_REGISTRATION

    companion object {
        const val BUSINESS_REGISTRATION = "BUSINESS_REGISTRATION"
    }
}

/** 내 서류 전체. [verified]면 필수 서류가 모두 승인돼 배차를 요청할 수 있다 */
data class DriverDocuments(
    val verified: Boolean,
    val approvedRequired: Int,
    val requiredTotal: Int,
    val items: List<DocumentItem>,
) {
    val required: List<DocumentItem> get() = items.filter { it.required }
    val optional: List<DocumentItem> get() = items.filter { !it.required }
    val pendingCount: Int get() = required.count { it.status == DocumentStatus.PENDING }
    val rejectedCount: Int get() = required.count { it.status == DocumentStatus.REJECTED }
    val missingCount: Int get() = required.count { it.latest == null }
}

/** 사업자등록증과 같이 보내는 값. 사업자번호를 비우면 서버가 계정의 사업자 정보를 쓴다 */
data class BusinessInfo(val businessNumber: String?, val openedOn: String)

/** 주선사가 보는 내 이력 (서버 /api/drivers/me/record). [verified]면 인증 마크 */
data class DriverRecord(val verified: Boolean, val completedTripCount: Long, val lateCancelCount: Long)

interface DocumentRepository {
    suspend fun documents(): ApiResult<DriverDocuments>

    suspend fun record(): ApiResult<DriverRecord> = ApiResult.Success(DriverRecord(false, 0, 0))

    suspend fun submit(type: String, jpeg: ByteArray, business: BusinessInfo?): ApiResult<Unit>
}
