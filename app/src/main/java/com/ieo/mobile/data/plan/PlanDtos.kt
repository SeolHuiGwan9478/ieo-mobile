package com.ieo.mobile.data.plan

import com.ieo.mobile.domain.plan.MyPlan
import com.ieo.mobile.domain.plan.PlanCode
import com.ieo.mobile.domain.plan.PlanOption
import com.ieo.mobile.domain.plan.PlanUsage
import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ChoosePlanRequest(val plan: String)

@Serializable
data class DriverPlanResponse(
    val plan: String,
    val planLabel: String,
    val price: Int,
    val trial: Boolean,
    val trialAvailable: Boolean,
    val renews: Boolean,
    val paidUntil: String? = null,
    val daysLeft: Int? = null,
    val usage: UsageResponse,
    val plans: List<OptionResponse>,
    val demoPayment: Boolean,
) {
    @Serializable
    data class UsageResponse(val waitProofsSent: Long, val waitProofLimit: Int? = null, val taxInvoicesThisMonth: Long, val taxInvoiceLimit: Int? = null)

    @Serializable
    data class OptionResponse(val code: String, val label: String, val price: Int, val highlights: List<String>)

    fun toDomain() = MyPlan(
        plan = PlanCode.valueOf(plan),
        label = planLabel,
        price = price,
        trial = trial,
        trialAvailable = trialAvailable,
        renews = renews,
        paidUntil = paidUntil?.let(LocalDate::parse),
        daysLeft = daysLeft,
        usage = PlanUsage(usage.waitProofsSent, usage.waitProofLimit, usage.taxInvoicesThisMonth, usage.taxInvoiceLimit),
        options = plans.map { PlanOption(PlanCode.valueOf(it.code), it.label, it.price, it.highlights) },
        demoPayment = demoPayment,
    )
}
