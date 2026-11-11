package com.ieo.mobile.ui.order

import com.ieo.mobile.domain.order.BrokerMetrics
import com.ieo.mobile.domain.order.Order
import com.ieo.mobile.ui.common.DayTimeFormatter
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.common.roundedToManwon
import com.ieo.mobile.ui.theme.IeoTag
import com.ieo.mobile.ui.theme.TagColor

/** 오더를 화면 문구로 바꾼다 */
class OrderTexts(private val labels: DayTimeFormatter = DayTimeFormatter()) {

    fun route(order: Order) = "${order.pickup.name} → ${order.dropoff.name}"

    /** "상차 내일 05:00~06:00 · 하차 내일 13:00까지" */
    fun schedule(order: Order) = "상차 ${pickupTime(order)} · 하차 ${dropoffTime(order)}"

    fun pickupTime(order: Order) = "${labels.dayTime(order.pickupFrom)}~${labels.time(order.pickupUntil)}"

    fun dropoffTime(order: Order) = "${labels.dayTime(order.dropoffBy)}까지"

    /** "상차지 18km". 위치를 모르면 null */
    /** 서버가 주는 거리는 도로 거리 어림이라 "약"을 붙인다. 상세의 실수익 내역은 실제 경로로 다시 잰다 */
    fun distance(order: Order): String? = order.distanceMeters?.let { if (it < 1_000) "1km 안" else "약 ${(it + 500) / 1_000}km" }

    /** "실수익 약 28만 원". 위치를 몰라 계산하지 못하면 null */
    fun netProfit(order: Order): String? = order.netProfit?.let { "실수익 약 ${formatWon(roundedToManwon(it))}" }

    /** "건축 자재 · 4.8톤". 화물 종류와 싣는 방법은 태그로 따로 보여 준다 (차량 앱과 같다) */
    fun cargo(order: Order): String = listOfNotNull(
        order.cargoType,
        order.weightKg.takeIf { it > 0 }?.let { "%.1f톤".format(it / 1000.0) },
    ).joinToString(" · ").ifEmpty { "정보 없음" }

    /** 싣고 내리는 방법: "지게차", "수작업". 색으로 좋고 나쁨을 말하지 않는다 */
    fun loadingTag(order: Order): OrderTag? = order.loadingMethodLabel?.takeIf { it.isNotBlank() }?.let { OrderTag(it, IeoTag.Gray) }

    /** 상세 제목 아래 태그: 차량, 일반이 아닌 화물 종류, 결제 방식. 주선사 태그는 이름 옆에 따로 둔다 */
    fun detailTags(order: Order): List<OrderTag> = listOfNotNull(
        OrderTag(order.vehicleLabel, IeoTag.Navy),
        order.specialCargo?.let(::cargoTag),
        OrderTag(order.paymentTypeLabel, paymentColor(order.paymentTypeLabel)),
    )

    fun metrics(metrics: BrokerMetrics): String {
        if (!metrics.published) return "거래 기록 적음"
        return listOfNotNull(
            metrics.avgPaymentDays?.let { "평균 지급 ${it}일" },
            metrics.latePaymentPercent?.let { "지급 지연 $it%" },
            metrics.waitFeePaidPercent?.let { "대기비 지급 $it%" },
            metrics.avgWaitMinutes?.let { "이 회사 운행 평균 대기 ${it}분" },
        ).joinToString(" · ").ifEmpty { "아직 기록이 없어요" }
    }

    fun tags(order: Order): List<OrderTag> = listOfNotNull(
        OrderTag(order.vehicleLabel, IeoTag.Navy),
        order.specialCargo?.let(::cargoTag),
        OrderTag(order.paymentTypeLabel, paymentColor(order.paymentTypeLabel)),
        order.brokerMetrics?.let(::brokerTag),
    )

    private fun paymentColor(type: String): TagColor = when (type) {
        "선불" -> IeoTag.Teal
        "인수증" -> IeoTag.Sand
        else -> IeoTag.Gray
    }

    private fun cargoTag(category: com.ieo.mobile.domain.order.CargoCategory) = OrderTag(category.label, if (category.name == "HAZARDOUS") IeoTag.Plum else IeoTag.Gray)

    /** 15일 이하 빠름, 20일 초과 늦음 (차량 앱과 같은 문구) */
    fun brokerTag(metrics: BrokerMetrics): OrderTag {
        val days = metrics.avgPaymentDays
        return when {
            !metrics.published -> OrderTag("거래 기록 적음", IeoTag.Gray)
            days == null -> OrderTag("지급 이력 없음", IeoTag.Gray)
            days <= 15 -> OrderTag("지급 평균 ${days}일", IeoTag.Sage)
            days > 20 -> OrderTag("지급 평균 ${days}일", IeoTag.Plum)
            else -> OrderTag("지급 평균 ${days}일", IeoTag.Gray)
        }
    }
}

data class OrderTag(val text: String, val color: TagColor)
