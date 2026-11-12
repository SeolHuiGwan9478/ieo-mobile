package com.ieo.mobile.ui.my

import com.ieo.mobile.domain.plan.MyPlan
import com.ieo.mobile.ui.plan.CurrentTicket
import com.ieo.mobile.ui.plan.TicketShape
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.R
import com.ieo.mobile.domain.auth.PhoneFormat
import com.ieo.mobile.domain.document.DriverDocuments
import com.ieo.mobile.domain.document.DriverRecord
import com.ieo.mobile.ui.component.FormField
import com.ieo.mobile.ui.component.LineGray
import com.ieo.mobile.ui.component.QuietButton
import com.ieo.mobile.ui.document.VerifiedBadge
import com.ieo.mobile.ui.document.documentsSummary
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import com.ieo.mobile.ui.vehicle.VehiclesState

/**
 * 내 정보 (M11). 위에서부터 프로필(그림·이름·인증 마크), 주선사에게 보이는 내 기록,
 * 자주 쓰는 바로가기 네 칸, 그리고 묶음별 메뉴. 메뉴마다 그림을 붙여 글을 덜 읽어도 찾게 한다
 */
@Composable
fun MyScreen(viewModel: MyViewModel, vehicles: VehiclesState, linkCount: Int, invitationCount: Int, recommendationSummary: String?, onVehicles: () -> Unit, onLinks: () -> Unit, onRecommendation: () -> Unit, carSessionCount: Int = 0, onAccountSection: (AccountSection) -> Unit = {}, documents: DriverDocuments? = null, onDocuments: () -> Unit = {}, record: DriverRecord? = null, plan: MyPlan? = null, onPlan: () -> Unit = {}, onCoupons: () -> Unit = {}, onEditProfile: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var withdrawing by remember { mutableStateOf(false) }
    val name = state.account?.name.orEmpty()
    val verified = documents?.verified == true

    Column(Modifier.fillMaxSize().background(White).verticalScroll(rememberScrollState())) {
        Text("내 정보", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp))

        // 프로필
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_avatar_driver), contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name.ifBlank { "이름 없음" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                    if (verified) VerifiedBadge(Modifier.padding(start = 8.dp))
                }
                Text(PhoneFormat.hyphen(state.account?.phone.orEmpty()), fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(top = 2.dp))
            }
            QuietButton("내 정보 수정", onEditProfile)
        }

        // 내 플랜 티켓 (크레딧 대신 30일 플랜, 2026-11-12)
        PlanCard(plan, onPlan, onCoupons)
        Spacer(Modifier.height(12.dp))

        // 주선사에게 보이는 내 기록
        Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().border(1.dp, LineGray, RoundedCornerShape(14.dp)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("주선사에게 보이는 내 기록", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMid, modifier = Modifier.weight(1f))
                Text(documents?.let(::documentsSummary).orEmpty(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Orange700, modifier = Modifier.clickable(onClick = onDocuments))
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Stat("${record?.completedTripCount ?: 0}건", "완료 운행", Modifier.weight(1f))
                Stat("${record?.lateCancelCount ?: 0}건", "직전 취소", Modifier.weight(1f))
                Stat("${linkCount}곳", "단골 주선사", Modifier.weight(1f))
            }
            if (!verified) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp).background(Orange50, RoundedCornerShape(10.dp)).clickable(onClick = onDocuments).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Orange600, modifier = Modifier.size(22.dp))
                    Text("서류를 올리면 인증 차주 마크가 붙어요", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Orange700, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Orange700)
                }
            }
        }

        // 바로가기 네 칸
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp)) {
            Shortcut(R.drawable.ic_menu_truck, "내 차량", vehicles.defaultVehicle?.label ?: "등록하기", Modifier.weight(1f), onVehicles)
            Shortcut(R.drawable.ic_menu_document, "서류", documents?.let(::documentsSummary).orEmpty(), Modifier.weight(1f), onDocuments)
            Shortcut(R.drawable.ic_menu_partner, "단골 주선사", if (invitationCount > 0) "요청 ${invitationCount}건" else "${linkCount}곳", Modifier.weight(1f), onLinks, highlight = invitationCount > 0)
            Shortcut(R.drawable.ic_menu_recommend, "배차 추천", recommendationSummary.orEmpty(), Modifier.weight(1f), onRecommendation)
        }
        Divider()

        Group("설정")
        MenuRow(R.drawable.ic_menu_car_login, "로그인된 차량", "${carSessionCount}대") { onAccountSection(AccountSection.CARS) }
        MenuRow(R.drawable.ic_menu_consent, "동의 관리", "") { onAccountSection(AccountSection.CONSENTS) }
        MenuRow(R.drawable.ic_menu_business, "사업자 정보", "선택") { onAccountSection(AccountSection.BUSINESS) }

        state.message?.let { Text(it, fontSize = 16.sp, color = IeoStatus.colors.warning, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            QuietButton("로그아웃", viewModel::signOut)
            QuietButton("회원 탈퇴", { withdrawing = true })
        }
    }

    if (withdrawing) {
        AlertDialog(
            onDismissRequest = { withdrawing = false },
            title = { Text("정말 탈퇴할까요?") },
            text = { Text("개인정보와 위치 기록이 지워지고 되돌릴 수 없어요.", color = TextMid) },
            confirmButton = {
                TextButton(onClick = { withdrawing = false; viewModel.withdraw() }) { Text("탈퇴", color = IeoStatus.colors.warning) }
            },
            dismissButton = { TextButton(onClick = { withdrawing = false }) { Text("취소", color = TextMid) } },
        )
    }
}

/** 지금 내 플랜 티켓. 누르면 플랜 화면 (PLAN-007). 아직 못 불러왔으면 자리만 둔다 */
@Composable
private fun PlanCard(plan: MyPlan?, onPlan: () -> Unit, onCoupons: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("내 플랜", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.weight(1f))
            // 쓸 수 있는 보상 쿠폰이 있으면 쿠폰함으로 바로 (DSP-034)
            val usable = plan?.coupons?.count { it.usable } ?: 0
            if (usable > 0) TextButton(onClick = onCoupons) { Text("쿠폰 ${usable}장", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Orange700) }
            TextButton(onClick = onPlan) { Text("플랜 보기", fontSize = 15.sp, color = TextSub) }
        }
        if (plan != null) {
            CurrentTicket(plan, Modifier.clip(TicketShape()).clickable(onClick = onPlan))
        } else {
            Box(Modifier.fillMaxWidth().height(150.dp).background(CanvasGray, TicketShape()).clickable(onClick = onPlan))
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        Text(label, fontSize = 14.sp, color = TextSub, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Shortcut(icon: Int, label: String, value: String, modifier: Modifier, onClick: () -> Unit, highlight: Boolean = false) {
    Column(modifier.clickable(onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(Modifier.size(52.dp).background(CanvasGray, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(30.dp))
            }
            if (highlight) Box(Modifier.align(Alignment.TopEnd).size(10.dp).background(Orange600, CircleShape))
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextStrong, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        // 메뉴 목록을 따로 두지 않고 지금 값을 칸 아래에 보여 준다
        Text(value, fontSize = 13.sp, color = if (highlight) Orange700 else TextSub, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun Group(title: String) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp))
}

@Composable
private fun MenuRow(icon: Int, label: String, value: String, highlight: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 16.sp, color = TextStrong, modifier = Modifier.weight(1f).padding(start = 14.dp))
        Text(value, fontSize = 15.sp, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal, color = if (highlight) Orange700 else TextSub)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = TextSub, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(8.dp).background(CanvasGray.copy(alpha = 0.6f)))
}

