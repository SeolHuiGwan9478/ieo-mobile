package com.ieo.mobile.domain.trip

import com.ieo.mobile.core.network.ApiResult
import java.time.Instant

/** 운행 상태. 진행은 차량 앱이 맡고, 폰은 보여 주기만 한다 */
enum class TripStatus(val label: String) {
    SCHEDULED("예정"),
    TO_PICKUP("상차지로 가는 중"),
    AT_PICKUP("상차지 도착"),
    IN_TRANSIT("하차지로 가는 중"),
    AT_DROPOFF("하차지 도착"),
    COMPLETED("완료"),
    CANCELLED("취소됨");

    val isInProgress: Boolean get() = this in setOf(TO_PICKUP, AT_PICKUP, IN_TRANSIT, AT_DROPOFF)
    val isFinished: Boolean get() = this == COMPLETED || this == CANCELLED
}

enum class StopType { PICKUP, DROPOFF }

data class TripStop(val name: String, val address: String, val plannedAt: Instant?, val siteId: Long? = null)

data class Trip(
    val id: Long,
    val status: TripStatus,
    val fare: Long,
    val paymentTypeLabel: String,
    val brokerName: String?,
    val plateNumber: String,
    val pickup: TripStop,
    val dropoff: TripStop,
    /** 차주가 직접 등록한 운행 */
    val isManual: Boolean,
    val createdAt: Instant,
) {
    /** 이에오 배차는 사유를 받아야 해서 폰에서는 직접 등록한 예정 운행만 취소한다 */
    val canCancel: Boolean get() = isManual && status == TripStatus.SCHEDULED

    /** 사진을 올리면 인수가 끝난 것으로 치고 되돌릴 수 없어, 하차지에 도착한 뒤에만 연다 (POD-009) */
    val canUploadPaperProof: Boolean get() = status == TripStatus.AT_DROPOFF || status == TripStatus.COMPLETED
}

/** 서버 PaymentType과 같은 이름 */
enum class PaymentType(val label: String) { RECEIPT("인수증"), PREPAID("선불"), COLLECT("착불") }

/** 상하차지 고르기. 이미 있는 곳은 id로, 처음 가는 곳은 좌표로 보낸다 */
sealed interface StopChoice {
    val name: String
    val address: String

    data class Known(val siteId: Long, override val name: String, override val address: String) : StopChoice

    data class New(override val name: String, override val address: String, val latitude: Double, val longitude: Double) : StopChoice
}

data class NewTrip(
    val pickup: StopChoice,
    val dropoff: StopChoice,
    /** 상차 예정 시각. 모르면 null */
    val pickupAt: Instant?,
    val fare: Long,
    val paymentType: PaymentType,
    val brokerName: String?,
)

/** 다녀온 곳을 고를 수 있도록 siteId까지 함께 준다 */
data class VisitedSite(val siteId: Long, val name: String, val address: String)

interface TripRepository {

    suspend fun list(): ApiResult<List<Trip>>

    /** 내 운행 기록에 나온 상하차지. 최근 순 */
    suspend fun visitedSites(): ApiResult<List<VisitedSite>>

    suspend fun register(trip: NewTrip): ApiResult<Unit>

    suspend fun cancel(tripId: Long): ApiResult<Unit>
}

/** 주소로 장소 찾기 */
interface PlaceSearch {

    suspend fun search(query: String): List<StopChoice.New>

    /** 지금 위치의 장소. 모르면 null */
    suspend fun here(): StopChoice.New?
}
