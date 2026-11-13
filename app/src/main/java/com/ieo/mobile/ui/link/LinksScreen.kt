package com.ieo.mobile.ui.link

import com.ieo.mobile.ui.theme.Gray100
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.order.OrderTexts
import androidx.compose.material.icons.outlined.Info
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import com.ieo.mobile.ui.component.ruled
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

/** 수락이 곧 위치 공유 동의라서 무엇을 공유하는지 받은 요청 위에 한 번 적는다 */
private const val CONSENT_TEXT = "수락하면 그 회사 일을 하는 동안 내 위치와 도착 예정 시각이 그 회사에 공유돼요. 그 회사가 나에게만 보이는 배차를 보낼 수 있어요."

@Composable
fun LinksScreen(viewModel: LinksViewModel, onBack: () -> Unit, labels: DayTimeFormatter = remember { DayTimeFormatter() }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val texts = remember { OrderTexts() }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("단골 주선사", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp)) {
            state.notice?.let { NoticeBar(LedgerNotice(it), viewModel::dismissNotice) }
            state.error?.let { ErrorNotice(it, if (state.loadFailed) viewModel::refresh else null) }
            if (state.isLoading && state.links.isEmpty() && state.invitations.isEmpty()) SkeletonCards(2)
            if (state.links.isEmpty() && state.invitations.isEmpty() && !state.isLoading && !state.loadFailed) {
                EmptyState("연결된 회사가 없어요", "주선사가 내 번호로 요청하면 여기에 나와요")
            }
            if (state.invitations.isNotEmpty()) {
                SectionHeader("받은 요청", state.invitations.size)
                ConsentCard()
                Spacer(Modifier.height(12.dp))
                state.invitations.forEach { InvitationCard(it, state.isBusy, viewModel, labels, texts) }
            }
            if (state.links.isNotEmpty()) {
                SectionHeader("연결된 회사", state.links.size)
                state.links.forEach { LinkRow(it, state.isBusy, viewModel, labels) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        Text(" $count", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Orange700)
    }
}

/** 받은 요청 위에 한 번만 두는 공유 안내 */
@Composable
private fun ConsentCard() {
    Row(
        Modifier.fillMaxWidth().background(Orange50, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = Orange700, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("수락하면 위치를 공유해요", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            Spacer(Modifier.height(4.dp))
            Text(CONSENT_TEXT, fontSize = 14.sp, lineHeight = 20.sp, color = TextMid)
            Spacer(Modifier.height(4.dp))
            Text("연결한 뒤에도 여기서 언제든 해제할 수 있어요.", fontSize = 14.sp, lineHeight = 20.sp, color = TextSub)
        }
    }
}

/** 받은 요청 하나: 회사, 신뢰 지표, 같이 한 운행, 기한, 거절·수락 */
@Composable
private fun InvitationCard(link: DriverLink, busy: Boolean, viewModel: LinksViewModel, labels: DayTimeFormatter, texts: OrderTexts) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 10.dp)
            .border(1.dp, Gray100, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(link.brokerName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            link.brokerMetrics?.let { metrics ->
                val tag = texts.brokerTag(metrics)
                Text(
                    tag.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = tag.color.content,
                    modifier = Modifier.background(tag.color.background, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val together = if (link.tripsTogether > 0) "같이 한 운행 ${link.tripsTogether}건" else "같이 한 운행 없음"
        val until = link.expiresAt?.let { "${labels.day(it)}까지 답할 수 있어요" }
        Text(listOfNotNull(together, until).joinToString(" · "), fontSize = 14.sp, color = TextSub)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TonalButton("거절", { viewModel.decline(link) }, enabled = !busy, modifier = Modifier.weight(1f))
            PrimaryButton("수락", { viewModel.accept(link) }, busy = busy, modifier = Modifier.weight(2f))
        }
    }
}

@Composable
private fun LinkRow(link: DriverLink, busy: Boolean, viewModel: LinksViewModel, labels: DayTimeFormatter) {
    var unlinking by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().ruled().padding(start = 20.dp, end = 10.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(link.brokerName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            val since = link.linkedAt?.let { "${labels.day(it)}부터 연결" }
            val together = if (link.tripsTogether > 0) "같이 한 운행 ${link.tripsTogether}건" else null
            listOfNotNull(since, together).takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" · "), fontSize = 15.sp, color = TextSub) }
        }
        QuietButton("해제", { if (!busy) unlinking = true })
    }
    if (unlinking) {
        AlertDialog(
            onDismissRequest = { unlinking = false },
            title = { Text("${link.brokerName} 연결을 해제할까요?") },
            text = { Text("위치 공유와 새 지정 배차가 끝나요. 이미 잡은 배차는 그대로예요.", fontSize = 16.sp, color = TextMid) },
            confirmButton = { TextButton(onClick = { unlinking = false; viewModel.unlink(link) }) { Text("해제", fontSize = 16.sp, color = IeoStatus.colors.warning) } },
            dismissButton = { TextButton(onClick = { unlinking = false }) { Text("취소", fontSize = 16.sp, color = TextMid) } },
        )
    }
}
