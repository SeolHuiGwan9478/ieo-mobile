package com.ieo.mobile.domain.order

import java.time.Instant

/** 오더 마켓에 올라온 오더 (서버 OrderResponse) */
data class Order(
    val id: Long,
    val brokerName: String,
    val pickup: OrderSite,
    val dropoff: OrderSite,
    /** 상차 가능 시간 */
    val pickupFrom: Instant,
    val pickupUntil: Instant,
    /** 이때까지 하차 */
    val dropoffBy: Instant,
    /** "5톤", "윙바디" 같은 서버 표기 */
    val tonnageLabel: String,
    val bodyTypeLabel: String,
    /** 화물 종류. 주선사가 적지 않았으면 null */
    val cargoType: String?,
    val weightKg: Int,
    /** "지게차", "수작업". 정하지 않았으면 null */
    val loadingMethodLabel: String?,
    val fare: Long,
    val paymentTypeLabel: String,
    /** 운행 뒤 며칠 안에 지급하는지 */
    val expectedPaymentDays: Int,
    /** 위치를 보내고 찾았을 때만: 지금 위치에서 상차지까지 거리 */
    val distanceMeters: Long?,
    /** 내가 이 오더에 보낸 배차 요청. 없으면 null */
    val myRequest: DispatchRequest?,
    /** 화물 종류(일반, 냉동·냉장, 이사, 위험물). 모르는 값이면 null */
    val cargoCategory: CargoCategory? = null,
    /** 주선사 신뢰 지표. 서버가 주지 않으면 null */
    val brokerMetrics: BrokerMetrics? = null,
    /** 위치를 보내고 찾았을 때만: 운임에서 기름값을 뺀 어림값 */
    val netProfit: Long? = null,
) {
    val vehicleLabel: String get() = "$tonnageLabel $bodyTypeLabel"

    /** 일반이 아닌 화물 종류. 목록에 태그로 붙인다 (일반은 대부분이라 붙이지 않는다) */
    val specialCargo: CargoCategory? get() = cargoCategory?.takeIf { it != CargoCategory.GENERAL }

    /** 확정을 기다리는 내 요청이 있음 */
    val isRequestedByMe: Boolean get() = myRequest?.status == DispatchRequestStatus.PENDING
}

/** 오더 상세의 실수익 내역. 통행료를 계산하지 못하면 [tollFare]가 null이다 */
data class OrderProfit(val netProfit: Long, val fuelCost: Long, val tollFare: Long?, val emptyMeters: Long)

data class OrderSite(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val siteId: Long? = null,
)

/** 내가 보낸 배차 요청. 10분 안에 주선사가 확정하지 않으면 만료된다 (DSP-023) */
data class DispatchRequest(
    val id: Long,
    val orderId: Long,
    val status: DispatchRequestStatus,
    val expiresAt: Instant,
)

enum class DispatchRequestStatus {
    PENDING,
    CONFIRMED,
    DECLINED,
    WITHDRAWN,
    CANCELED,
    EXPIRED,
}

/** 오더 신고 사유 (DSP-042) */
enum class ReportType(val label: String) {
    FALSE_INFO("허위 오더"),
    OVERLOAD("과적 요구"),
    UNPAID("운임 미지급"),
    MISMATCH("정보가 실제와 다름"),
}
