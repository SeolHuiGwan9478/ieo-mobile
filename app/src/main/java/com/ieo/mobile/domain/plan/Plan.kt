package com.ieo.mobile.domain.plan

import com.ieo.mobile.core.network.ApiResult
import java.time.LocalDate

/** 기사님 플랜 (12장 PLAN-007, 008). 30일 단위, 결제는 데모 */
enum class PlanCode { BASIC, PRO }

/** 플랜 하나. highlights는 티켓에 적는 세 줄 */
data class PlanOption(val code: PlanCode, val label: String, val price: Int, val highlights: List<String>)

/**
 * 보상 쿠폰 (주선사 늦은 취소). [사용하기]를 눌러야 프로 이용일이 더해진다.
 * used면 쓴 것, used도 usable도 아니면 기간이 지난 것
 */
data class PlanCoupon(val id: Long, val days: Int, val reason: String, val issuedOn: LocalDate, val expiresOn: LocalDate, val used: Boolean, val usable: Boolean)

/** 한도. null이면 무제한 */
data class PlanUsage(val waitProofsSent: Long, val waitProofLimit: Int?, val taxInvoicesThisMonth: Long, val taxInvoiceLimit: Int?)

/**
 * 내 플랜. paidUntil은 유료 기간이 끝나는 날, daysLeft는 그날까지 남은 날(베이직이면 둘 다 null).
 * renews는 기간이 끝나면 다시 결제(데모)되는지. 체험·해지면 false
 */
data class MyPlan(
    val plan: PlanCode,
    val label: String,
    val price: Int,
    val trial: Boolean,
    val trialAvailable: Boolean,
    val renews: Boolean,
    /** 쿠폰으로 받은 프로 이용일로만 쓰는 중. 끝나면 결제 없이 베이직 */
    val gift: Boolean,
    val paidUntil: LocalDate?,
    val daysLeft: Int?,
    val usage: PlanUsage,
    val options: List<PlanOption>,
    val demoPayment: Boolean,
    /** 보상 쿠폰, 최근 받은 것 먼저 */
    val coupons: List<PlanCoupon> = emptyList(),
) {
    val paid: Boolean get() = plan != PlanCode.BASIC
}

interface PlanRepository {

    suspend fun mine(): ApiResult<MyPlan>

    /** 유료면 데모 결제 후 바로 적용, BASIC이면 해지(남은 날까지 쓴다) */
    suspend fun choose(code: PlanCode): ApiResult<MyPlan>

    /** 프로 30일 무료 체험 (한 번만) */
    suspend fun startTrial(): ApiResult<MyPlan>

    /** 해지 취소 */
    suspend fun resume(): ApiResult<MyPlan>

    /** 보상 쿠폰 쓰기: 프로 이용일이 지금 플랜 끝에 더해진다 */
    suspend fun useCoupon(couponId: Long): ApiResult<MyPlan>
}
