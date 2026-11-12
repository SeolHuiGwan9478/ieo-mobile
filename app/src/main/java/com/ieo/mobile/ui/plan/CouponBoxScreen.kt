package com.ieo.mobile.ui.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.domain.plan.MyPlan
import com.ieo.mobile.domain.plan.PlanCoupon
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val COUPON_STUB = 88.dp
private val COUPON_LINE = Color(0xFFFFD2C0)
private val USED_LINE = Color(0xFFE5E8EB)
private val USED_INK = Color(0xFFA3A3A3)

/**
 * 쿠폰함 (DSP-034 보상 쿠폰). 쿠폰함 흔한 모양을 따른다: 위에 "쓸 수 있는 쿠폰 / 지난 쿠폰" 탭, 아래 티켓 모양 쿠폰.
 * 쿠폰 시안은 docs/design/assets/plan-tickets/coupon-pro-days*.svg. [사용하기]를 누르면 한 번 더 묻고 그 기사님 플랜만 바꾼다
 */
@Composable
fun CouponBoxScreen(viewModel: PlanViewModel, state: PlanState, onBack: () -> Unit, today: LocalDate = LocalDate.now()) {
    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) { if (state.plan == null) viewModel.load() }
    var showUsed by rememberSaveable { mutableStateOf(false) }
    var confirming by remember { mutableStateOf<PlanCoupon?>(null) }
    var showingGuide by rememberSaveable { mutableStateOf(false) }
    if (showingGuide) {
        PlanGuideScreen(onBack = { showingGuide = false })
        return
    }
    val coupons = state.plan?.coupons.orEmpty()
    val usable = coupons.filter { it.usable }
    val past = coupons.filterNot { it.usable }

    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("쿠폰함", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            TabLabel("쓸 수 있는 쿠폰 ${usable.size}", selected = !showUsed, modifier = Modifier.weight(1f)) { showUsed = false }
            TabLabel("지난 쿠폰 ${past.size}", selected = showUsed, modifier = Modifier.weight(1f)) { showUsed = true }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            val shown = if (showUsed) past else usable
            Spacer(Modifier.height(16.dp))
            if (shown.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (showUsed) "지난 쿠폰이 없어요" else "쓸 수 있는 쿠폰이 없어요", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                    Text("주선사가 상차 2시간 안에 배차를 취소하면\n프로 3일 쿠폰이 들어와요", fontSize = 15.sp, color = TextSub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                }
            }
            shown.forEach { coupon ->
                CouponTicket(coupon, today, busy = state.busy, onUse = { confirming = coupon })
                Spacer(Modifier.height(12.dp))
            }
            Text("쿠폰은 받은 날부터 90일 안에 써야 해요. 쓰면 지금 플랜 끝에 프로 이용일이 더해지고, 베이직이면 바로 프로가 돼요.",
                fontSize = 14.sp, lineHeight = 21.sp, color = TextSub, modifier = Modifier.padding(top = 8.dp))
            Row(
                Modifier.padding(top = 16.dp, bottom = 24.dp).clip(RoundedCornerShape(10.dp)).clickable { showingGuide = true }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(20.dp).border(1.5.dp, TextSub, CircleShape), contentAlignment = Alignment.Center) {
                    Text("?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSub)
                }
                Text("쿠폰·요금 안내", fontSize = 15.sp, color = TextMid, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (state.message != null || state.error != null) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().background(CanvasGray, RoundedCornerShape(12.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.error ?: state.message.orEmpty(), fontSize = 15.sp, color = TextStrong, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::clearMessage) { Text("닫기", color = TextMid) }
            }
        }
    }

    confirming?.let { coupon ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text("프로 ${coupon.days}일 쿠폰을 쓸까요?") },
            text = { Text(useHint(state.plan, coupon, today), color = TextMid) },
            confirmButton = { TextButton(onClick = { confirming = null; viewModel.useCoupon(coupon) }) { Text("사용하기", color = Orange700, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text("나중에", color = TextMid) } },
        )
    }
}

/** 쓰면 언제까지 프로인지 미리 알려 준다 */
private fun useHint(plan: MyPlan?, coupon: PlanCoupon, today: LocalDate): String {
    val base = if (plan?.paid == true && plan.paidUntil != null) plan.paidUntil else today
    val until = base.plusDays(coupon.days.toLong())
    return if (plan?.paid == true) "지금 프로 기간 끝에 ${coupon.days}일이 더해져 ${dayLabel(until)}까지 써요."
    else "지금 바로 프로가 되고 ${dayLabel(until)}까지 써요. 끝나면 결제 없이 베이직으로 돌아가요."
}

@Composable
private fun TabLabel(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) TextStrong else TextSub)
        Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(if (selected) 2.dp else 1.dp).background(if (selected) TextStrong else USED_LINE))
    }
}

/** 쿠폰 한 장 (시안 coupon-pro-days.svg): 왼쪽 혜택·이유·기한, 떼는 선 오른쪽에 [사용하기] 또는 "사용 완료" */
@Composable
fun CouponTicket(coupon: PlanCoupon, today: LocalDate, busy: Boolean, onUse: () -> Unit) {
    val active = coupon.usable
    val line = if (active) COUPON_LINE else USED_LINE
    val shape = TicketShape(stubWidth = COUPON_STUB, notch = 10.dp, corner = 14.dp)
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 124.dp)
            .background(if (active) White else CanvasGray, shape).border(1.5.dp, line, shape)
            .drawBehind {
                val x = size.width - COUPON_STUB.toPx()
                val inset = 16.dp.toPx()
                drawLine(line, Offset(x, inset), Offset(x, size.height - inset), strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
            },
    ) {
        Column(Modifier.weight(1f).padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp)) {
            Text("프로 이용권", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (active) Orange600 else TextSub,
                modifier = Modifier.background(if (active) Orange50 else USED_LINE, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 2.dp))
            Text("프로 ${coupon.days}일", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = if (active) Orange600 else USED_INK, modifier = Modifier.padding(top = 6.dp))
            Text("${coupon.reason} 보상", fontSize = 14.sp, color = if (active) TextStrong else TextSub, modifier = Modifier.padding(top = 2.dp))
            val days = ChronoUnit.DAYS.between(today, coupon.expiresOn)
            val note = when {
                coupon.used -> "사용했어요"
                !active -> "${dotted(coupon.expiresOn)} 기간이 지났어요"
                else -> "${dotted(coupon.expiresOn)}까지 · D-$days"
            }
            Text(note, fontSize = 13.sp, color = if (active && days <= 7) Orange700 else TextSub, modifier = Modifier.padding(top = 4.dp))
        }
        Column(Modifier.width(COUPON_STUB).fillMaxHeight(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (active) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Orange600).clickable(enabled = !busy, onClick = onUse),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Check, contentDescription = "쿠폰 사용하기", tint = White) }
                Text("사용하기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Orange600, modifier = Modifier.padding(top = 6.dp))
            } else {
                Box(Modifier.size(56.dp).border(2.dp, USED_LINE, CircleShape), contentAlignment = Alignment.Center) {
                    Text(if (coupon.used) "사용\n완료" else "기간\n만료", fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, color = TextSub, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private fun dotted(day: LocalDate) = "%d.%02d.%02d".format(day.year, day.monthValue, day.dayOfMonth)
