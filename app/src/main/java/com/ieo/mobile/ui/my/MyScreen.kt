package com.ieo.mobile.ui.my

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.domain.auth.PhoneFormat
import com.ieo.mobile.ui.component.FormField
import com.ieo.mobile.ui.component.QuietButton
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange100
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import com.ieo.mobile.ui.vehicle.VehicleSummaryRow
import com.ieo.mobile.ui.vehicle.VehiclesState

/**
 * 내 정보 (M11). 시안의 연결된 구글 계정·로그인된 차량·사업자 정보·동의 관리·알림·고객센터는
 * 서버 API나 다른 도메인이 준비되면 더한다
 */
@Composable
fun MyScreen(viewModel: MyViewModel, vehicles: VehiclesState, linkCount: Int, invitationCount: Int, recommendationSummary: String?, onVehicles: () -> Unit, onLinks: () -> Unit, onRecommendation: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf(false) }
    var withdrawing by remember { mutableStateOf(false) }
    val name = state.account?.name.orEmpty()

    Column(
        Modifier.fillMaxSize().background(CanvasGray).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).background(Orange100, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Text(name.take(1), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Orange700)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (name.isBlank()) "내 정보" else "$name 님", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "차주",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange700,
                        modifier = Modifier.background(Orange50, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                QuietButton("이름 바꾸기", { renaming = true })
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(PhoneFormat.hyphen(state.account?.phone.orEmpty()), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                    Text("계정을 구분하는 번호예요", fontSize = 15.sp, color = TextSub)
                }
                Text("인증됨", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IeoStatus.colors.done)
            }
        }
        Card { VehicleSummaryRow(vehicles, onVehicles) }
        Card {
            Row(Modifier.fillMaxWidth().clickable(onClick = onLinks).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("단골 주선사", fontSize = 16.sp, color = TextStrong, modifier = Modifier.weight(1f))
                Text(if (invitationCount > 0) "요청 ${invitationCount}건" else "${linkCount}곳", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (invitationCount > 0) Orange700 else TextSub)
                Text("  ›", fontSize = 20.sp, color = TextSub)
            }
        }
        Card {
            Row(Modifier.fillMaxWidth().clickable(onClick = onRecommendation).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("다음 배차 추천", fontSize = 16.sp, color = TextStrong, modifier = Modifier.weight(1f))
                Text(recommendationSummary.orEmpty(), fontSize = 16.sp, color = TextSub)
                Text("  ›", fontSize = 20.sp, color = TextSub)
            }
        }
        state.message?.let { Text(it, fontSize = 16.sp, color = IeoStatus.colors.warning) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            QuietButton("로그아웃", viewModel::signOut)
            QuietButton("탈퇴하기", { withdrawing = true })
        }
    }

    if (renaming) RenameDialog(name, state.isBusy, viewModel) { renaming = false }
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

@Composable
private fun RenameDialog(current: String, busy: Boolean, viewModel: MyViewModel, onClose: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("이름 바꾸기") },
        text = { FormField(name, { name = it.take(30) }, "이름") },
        confirmButton = {
            TextButton(onClick = { viewModel.rename(name, onClose) }, enabled = name.isNotBlank() && !busy) { Text("저장", color = Orange700) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("취소", color = TextMid) } },
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(White, RoundedCornerShape(20.dp)).padding(horizontal = 20.dp, vertical = 18.dp)) { content() }
}
