package com.ieo.mobile.ui.order

import com.ieo.mobile.domain.order.CargoCategory
import com.ieo.mobile.domain.order.OrderSearch

/** 내 주변 거리. [km]가 null이면 위치 없이 전국에서 찾는다 */
enum class OrderRadius(val km: Int?, val label: String) {
    KM20(20, "20km"),
    KM50(50, "50km"),
    KM100(100, "100km"),
    NATIONWIDE(null, "전국"),
}

enum class DayFilter(val label: String) { ALL("전체"), TODAY("오늘"), TOMORROW("내일") }

enum class MinFare(val won: Long?, val label: String) {
    ANY(null, "전체"),
    OVER_30(300_000, "30만 원 이상"),
    OVER_50(500_000, "50만 원 이상"),
}

/** 결제 방식. [serverName]은 서버 PaymentType 이름 */
enum class PaymentFilter(val serverName: String?, val label: String) {
    ANY(null, "전체"),
    PREPAID("PREPAID", "선불"),
    RECEIPT("RECEIPT", "인수증"),
}

/**
 * 오더 목록 조건 (DSP-010). 거리·운임·결제는 서버가 거르고, 날짜·제외할 화물은 받은 목록에서 거른다.
 * 기본값: 내 주변 50km, 나머지는 모두 전체
 */
data class OrderConditions(
    val radius: OrderRadius = OrderRadius.KM50,
    val day: DayFilter = DayFilter.ALL,
    val minFare: MinFare = MinFare.ANY,
    val payment: PaymentFilter = PaymentFilter.ANY,
    val excluded: Set<CargoCategory> = emptySet(),
) {
    val search: OrderSearch get() = OrderSearch(radiusKm = radius.km ?: 50, minFare = minFare.won, paymentType = payment.serverName)

    /** 기본값에서 바꾼 조건 수. "조건" 버튼 옆 숫자 */
    val changedCount: Int get() = listOf(
        radius != OrderRadius.KM50,
        day != DayFilter.ALL,
        minFare != MinFare.ANY,
        payment != PaymentFilter.ANY,
        excluded.isNotEmpty(),
    ).count { it }

    /** 목록 위에 남기는 조건. 거리는 늘 보여 주고, 나머지는 바꾼 것만 */
    val summary: List<String> get() = listOfNotNull(
        radius.km?.let { "내 주변 ${radius.label}" } ?: "전국",
        day.takeIf { it != DayFilter.ALL }?.let { "${it.label} 상차" },
        minFare.takeIf { it != MinFare.ANY }?.label,
        payment.takeIf { it != PaymentFilter.ANY }?.label,
        excluded.takeIf { it.isNotEmpty() }?.let { set -> set.sortedBy { it.ordinal }.joinToString(", ") { it.label } + " 제외" },
    )
}
