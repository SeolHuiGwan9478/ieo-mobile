package com.ieo.mobile.ui.link

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ieo.mobile.domain.link.DriverLink
import com.ieo.mobile.ui.common.DayTimeFormatter
import com.ieo.mobile.ui.component.EmptyState
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.QuietButton
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.ledger.NoticeBar
import com.ieo.mobile.ui.ledger.LedgerNotice
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

private val CardShape = RoundedCornerShape(20.dp)

/** 수락이 곧 위치 공유 동의라서 무엇을 공유하는지 버튼 바로 위에 적는다 */
private const val CONSENT_TEXT = "수락하면 이 회사 일을 하는 동안 위치와 도착 예정 시각을 공유해요. 나에게만 보이는 배차도 받을 수 있어요."

@Composable
fun LinksScreen(viewModel: LinksViewModel, onBack: () -> Unit, labels: DayTimeFormatter = remember { DayTimeFormatter() }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(CanvasGray).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("단골 주선사", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.notice?.let { NoticeBar(LedgerNotice(it), viewModel::dismissNotice) }
            state.error?.let { ErrorNotice(it, if (state.loadFailed) viewModel::refresh else null) }
            if (state.isLoading && state.links.isEmpty() && state.invitations.isEmpty()) SkeletonCards(2)
            if (state.links.isEmpty() && state.invitations.isEmpty() && !state.isLoading && !state.loadFailed) {
                EmptyState("연결된 회사가 없어요", "주선사가 내 번호로 요청하면 여기에 나와요")
            }
            state.invitations.forEach { InvitationCard(it, state.isBusy, viewModel, labels) }
            if (state.links.isNotEmpty()) Text("연결된 회사", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMid, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
            state.links.forEach { LinkRow(it, state.isBusy, viewModel, labels) }
        }
    }
}

@Composable
private fun InvitationCard(link: DriverLink, busy: Boolean, viewModel: LinksViewModel, labels: DayTimeFormatter) {
    Column(Modifier.fillMaxWidth().background(White, CardShape).padding(20.dp)) {
        Text("${link.brokerName}에서 단골 요청이 왔어요", fontSize = 19.sp, lineHeight = 27.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong)
        Spacer(Modifier.height(8.dp))
        Text(CONSENT_TEXT, fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
        link.expiresAt?.let { Text("${labels.dayTime(it)}까지 답할 수 있어요", fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TonalButton("거절", { viewModel.decline(link) }, enabled = !busy, modifier = Modifier.weight(1f))
            PrimaryButton("수락", { viewModel.accept(link) }, busy = busy, modifier = Modifier.weight(2f))
        }
    }
}

@Composable
private fun LinkRow(link: DriverLink, busy: Boolean, viewModel: LinksViewModel, labels: DayTimeFormatter) {
    var unlinking by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().background(White, CardShape).padding(start = 20.dp, end = 10.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(link.brokerName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            link.linkedAt?.let { Text("${labels.day(it)} 연결", fontSize = 15.sp, color = TextSub) }
        }
        QuietButton("끊기", { if (!busy) unlinking = true })
    }
    if (unlinking) {
        AlertDialog(
            onDismissRequest = { unlinking = false },
            title = { Text("${link.brokerName} 연결을 끊을까요?") },
            text = { Text("위치 공유와 새 지정 배차가 끝나요. 이미 잡은 배차는 그대로예요.", fontSize = 16.sp, color = TextMid) },
            confirmButton = { TextButton(onClick = { unlinking = false; viewModel.unlink(link) }) { Text("끊기", fontSize = 16.sp, color = IeoStatus.colors.warning) } },
            dismissButton = { TextButton(onClick = { unlinking = false }) { Text("취소", fontSize = 16.sp, color = TextMid) } },
        )
    }
}
