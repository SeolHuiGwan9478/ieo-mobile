package com.ieo.mobile.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.plan.MyPlan
import com.ieo.mobile.domain.plan.PlanCode
import com.ieo.mobile.domain.plan.PlanCoupon
import com.ieo.mobile.domain.plan.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** message는 방금 한 일을 알리는 한 줄, error는 실패 이유 */
data class PlanState(
    val plan: MyPlan? = null,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

/** 기사님 플랜 화면과 내 정보의 티켓 (PLAN-007, 008). 결제는 데모 */
class PlanViewModel(private val plans: PlanRepository) : ViewModel() {

    private val _state = MutableStateFlow(PlanState())
    val state: StateFlow<PlanState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = it.plan == null, error = null) }
        viewModelScope.launch {
            when (val result = plans.mine()) {
                is ApiResult.Success -> _state.update { it.copy(plan = result.data, loading = false) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.error.message) }
            }
        }
    }

    fun choose(code: PlanCode) = act({ plans.choose(code) }) { plan ->
        when {
            code == PlanCode.BASIC -> "해지했어요. ${plan.paidUntil?.let { "${it.monthValue}월 ${it.dayOfMonth}일" } ?: "기간 끝"}까지 ${plan.label}을 쓰고 베이직으로 바뀌어요"
            else -> "${plan.label} 플랜을 시작했어요"
        }
    }

    fun startTrial() = act({ plans.startTrial() }) { "프로 30일 무료 체험을 시작했어요" }

    fun resume() = act({ plans.resume() }) { "해지를 취소했어요. 계속 ${it.label}을 써요" }

    fun useCoupon(coupon: PlanCoupon) = act({ plans.useCoupon(coupon.id) }) { plan ->
        "프로 ${coupon.days}일을 더했어요. ${plan.paidUntil?.let { "${it.monthValue}월 ${it.dayOfMonth}일" } ?: ""}까지 프로를 써요"
    }

    fun clearMessage() = _state.update { it.copy(message = null, error = null) }

    private fun act(call: suspend () -> ApiResult<MyPlan>, done: (MyPlan) -> String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null, error = null) }
        viewModelScope.launch {
            when (val result = call()) {
                is ApiResult.Success -> _state.update { it.copy(plan = result.data, busy = false, message = done(result.data)) }
                is ApiResult.Failure -> _state.update { it.copy(busy = false, error = result.error.message) }
            }
        }
    }
}
