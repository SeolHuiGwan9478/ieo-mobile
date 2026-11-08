package com.ieo.mobile.ui.order

import com.ieo.mobile.domain.order.BrokerMetrics
import com.ieo.mobile.domain.order.Order
import com.ieo.mobile.ui.common.DayTimeFormatter
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
    fun distance(order: Order): String? = order.distanceMeters?.let { if (it < 1_000) "1km 안" else "${(it + 500) / 1_000}km" }

    /** "건축 자재 · 4.8톤 · 지게차". 일반이 아닌 종류는 앞에 붙인다 */
    fun cargo(order: Order): String = listOfNotNull(
        order.specialCargo?.label,
        order.cargoType,
        order.weightKg.takeIf { it > 0 }?.let { "%.1f톤".format(it / 1000.0) },
        order.loadingMethodLabel,
    ).joinToString(" · ").ifEmpty { "정보 없음" }

    fun metrics(metrics: BrokerMetrics): String {
        if (!metrics.published) return "기록 쌓는 중"
        return listOfNotNull(
            metrics.avgPaymentDays?.let { "평균 지급 ${it}일" },
            metrics.latePaymentPercent?.let { "지연 $it%" },
            metrics.waitFeePaidPercent?.let { "대기비 지급 $it%" },
            metrics.avgWaitMinutes?.let { "이 회사 운행 평균 대기 ${it}분" },
        ).joinToString(" · ").ifEmpty { "아직 기록이 없어요" }
    }

    fun tags(order: Order): List<OrderTag> = listOfNotNull(
        OrderTag(order.vehicleLabel, IeoTag.Navy),
        order.specialCargo?.let { OrderTag(it.label, if (it.name == "HAZARDOUS") IeoTag.Plum else IeoTag.Gray) },
        OrderTag(order.paymentTypeLabel, paymentColor(order.paymentTypeLabel)),
        order.brokerMetrics?.let(::brokerTag),
    )

    private fun paymentColor(type: String): TagColor = when (type) {
        "선불" -> IeoTag.Teal
        "인수증" -> IeoTag.Sand
        else -> IeoTag.Gray
    }

    /** 15일 이하 빠름, 20일 초과 늦음 */
    private fun brokerTag(metrics: BrokerMetrics): OrderTag {
        val days = metrics.avgPaymentDays
        return when {
            !metrics.published -> OrderTag("기록 쌓는 중", IeoTag.Gray)
            days == null -> OrderTag("지급 이력 없음", IeoTag.Gray)
            days <= 15 -> OrderTag("지급 ${days}일", IeoTag.Sage)
            days > 20 -> OrderTag("지급 ${days}일", IeoTag.Plum)
            else -> OrderTag("지급 ${days}일", IeoTag.Gray)
        }
    }
}

data class OrderTag(val text: String, val color: TagColor)
