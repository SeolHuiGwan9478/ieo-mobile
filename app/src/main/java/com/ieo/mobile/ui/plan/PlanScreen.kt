package com.ieo.mobile.ui.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.domain.plan.MyPlan
import com.ieo.mobile.domain.plan.PlanCode
import com.ieo.mobile.domain.plan.PlanOption
import com.ieo.mobile.ui.component.LineGray
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import java.time.LocalDate

/**
 * 플랜 (PLAN-007, 008, BILL-004, 007). 위에 지금 내 티켓과 상태 한 줄, 아래에 세 플랜 티켓과 고르는 단추.
 * 결제는 시연용이라 누르면 바로 바뀌고, 화면 위에 그렇다고 알린다
 */
@Composable
fun PlanScreen(viewModel: PlanViewModel, state: PlanState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { viewModel.load() }
    var confirmCancel by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("플랜", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        val plan = state.plan
        if (plan == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator(color = Orange600)
                else state.error?.let { Text(it, fontSize = 16.sp, color = TextSub) }
            }
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (plan.demoPayment) DemoNotice()

            Text("지금 내 플랜", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
            CurrentTicket(plan)
            StatusLine(plan, onResume = viewModel::resume)
            if (plan.usage.waitProofLimit != null || plan.usage.taxInvoiceLimit != null) UsageLine(plan)

            Text("플랜 고르기", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 28.dp, bottom = 4.dp))
            Text("30일 단위예요. 운행 건마다 돈을 받지 않아요.", fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(bottom = 12.dp))
            plan.options.forEach { option ->
                PlanTicket(option.code, option.label, option.highlights, priceLabel(option.price), if (option.price == 0) "늘 무료" else "30일",
                    badge = if (option.code == plan.plan) "쓰는 중" else null)
                OptionActions(option, plan, state.busy, onChoose = viewModel::choose, onTrial = viewModel::startTrial, onCancel = { confirmCancel = true })
                Spacer(Modifier.height(18.dp))
            }
        }
        if (state.message != null || state.error != null) {
            val isError = state.error != null
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth()
                    .background(if (isError) IeoStatus.colors.warning.copy(alpha = 0.12f) else CanvasGray, RoundedCornerShape(12.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.error ?: state.message.orEmpty(), fontSize = 15.sp, color = TextStrong, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::clearMessage) { Text("닫기", color = TextMid) }
            }
        }
    }

    if (confirmCancel) {
        val until = state.plan?.paidUntil?.let(::dayLabel) ?: "기간 끝"
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("베이직으로 내릴까요?") },
            text = { Text("$until 까지는 지금 플랜을 그대로 쓰고, 그 뒤 베이직으로 바뀌어요. 쌓인 장부는 지우지 않아요.", color = TextMid) },
            confirmButton = { TextButton(onClick = { confirmCancel = false; viewModel.choose(PlanCode.BASIC) }) { Text("해지", color = IeoStatus.colors.warning) } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("그대로 쓰기", color = TextMid) } },
        )
    }
}

@Composable
private fun DemoNotice() {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp).background(Orange50, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = Orange700, modifier = Modifier.size(20.dp))
        Text("시연용 결제예요. 누르면 바로 결제된 것으로 처리되고 실제 돈은 나가지 않아요.", fontSize = 14.sp, color = Orange700, modifier = Modifier.padding(start = 8.dp))
    }
}

/** 지금 내 티켓: 유료면 떼는 부분에 D-남은 날과 끝나는 날 */
@Composable
fun CurrentTicket(plan: MyPlan, modifier: Modifier = Modifier) {
    val highlights = plan.options.firstOrNull { it.code == plan.plan }?.highlights.orEmpty()
    val (title, sub) = if (plan.paid && plan.daysLeft != null && plan.paidUntil != null) {
        "D-${plan.daysLeft}" to "${plan.paidUntil.monthValue}.${plan.paidUntil.dayOfMonth}까지"
    } else {
        "무료" to "늘 무료"
    }
    PlanTicket(plan.plan, if (plan.trial) "${plan.label} 체험" else plan.label, highlights, title, sub, modifier, badge = "쓰는 중")
}

@Composable
private fun StatusLine(plan: MyPlan, onResume: () -> Unit) {
    val until = plan.paidUntil?.let(::dayLabel)
    val text = when {
        !plan.paid -> "베이직은 늘 무료예요. 필요한 기능이 생기면 프로로 바꿔 보세요."
        plan.trial -> "무료 체험 중이에요. ${until}에 끝나면 결제 없이 베이직으로 돌아가요."
        plan.renews -> "${until}에 ${priceLabel(plan.price)}이 다시 결제돼요 (시연용)."
        else -> "해지했어요. ${until}까지 쓰고 베이직으로 바뀌어요."
    }
    Text(text, fontSize = 15.sp, color = TextMid, modifier = Modifier.padding(top = 10.dp))
    if (plan.paid && !plan.trial && !plan.renews) {
        TextButton(onClick = onResume) { Text("해지 취소", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Orange700) }
    }
}

@Composable
private fun UsageLine(plan: MyPlan) {
    val usage = plan.usage
    Column(Modifier.fillMaxWidth().padding(top = 12.dp).border(1.dp, LineGray, RoundedCornerShape(12.dp)).padding(14.dp)) {
        usage.waitProofLimit?.let { Text("대기 증빙 보내기  최근 30일 ${usage.waitProofsSent} / ${it}건", fontSize = 15.sp, color = TextStrong) }
        usage.taxInvoiceLimit?.let { Text("세금계산서  이번 달 ${usage.taxInvoicesThisMonth} / ${it}장", fontSize = 15.sp, color = TextStrong, modifier = Modifier.padding(top = 6.dp)) }
    }
}

/** 플랜 티켓 아래 단추. 지금 플랜이면 없음(유료면 해지), 베이직에서 프로는 체험도 */
@Composable
private fun OptionActions(option: PlanOption, plan: MyPlan, busy: Boolean, onChoose: (PlanCode) -> Unit, onTrial: () -> Unit, onCancel: () -> Unit) {
    val current = option.code == plan.plan
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            current && plan.paid && plan.renews && !plan.trial -> TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("해지하기", fontSize = 15.sp, color = TextSub)
            }
            current -> Unit
            option.code == PlanCode.BASIC -> if (plan.renews) {
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("베이직으로 내리기", fontSize = 15.sp, color = TextSub) }
            }
            else -> {
                if (option.code == PlanCode.PRO && plan.trialAvailable && !plan.paid) {
                    PrimaryButton("30일 무료로 써 보기", onTrial, busy = busy)
                }
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).border(1.5.dp, TextStrong, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)).clickable(enabled = !busy) { onChoose(option.code) },
                    contentAlignment = Alignment.Center,
                ) { Text("${option.label}로 바꾸기 · ${priceLabel(option.price)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong) }
            }
        }
    }
}

internal fun dayLabel(day: LocalDate) = "${day.monthValue}월 ${day.dayOfMonth}일"
