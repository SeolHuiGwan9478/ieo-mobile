package com.ieo.mobile.domain.order

/**
 * 주선사 신뢰 지표 (BRK-030~032, DSP-011). 차주 장부와 대기 기록으로 서버가 계산한다.
 * 끝난 운행이 20건보다 적으면 [published]가 false이고 값은 모두 null ("기록 쌓는 중").
 * 공개돼도 해당 건이 없는 지표는 null
 */
data class BrokerMetrics(
    val published: Boolean,
    val avgPaymentDays: Int? = null,
    val latePaymentPercent: Int? = null,
    val waitFeePaidPercent: Int? = null,
    val avgWaitMinutes: Int? = null,
)
