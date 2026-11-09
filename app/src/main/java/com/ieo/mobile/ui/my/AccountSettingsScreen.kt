package com.ieo.mobile.ui.my

import com.ieo.mobile.ui.component.ruled
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.domain.account.CarSession
import com.ieo.mobile.domain.account.ConsentItem
import com.ieo.mobile.domain.auth.Consent
import com.ieo.mobile.ui.common.DayTimeFormatter
import com.ieo.mobile.ui.component.EmptyState
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.FormField
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.QuietButton
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.ledger.LedgerNotice
import com.ieo.mobile.ui.ledger.NoticeBar
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

/** 내 정보에서 여는 설정 화면 세 가지 */
@Composable
fun AccountSettingsScreen(
    section: AccountSection,
    viewModel: AccountSettingsViewModel,
    onBack: () -> Unit,
    labels: DayTimeFormatter = remember { DayTimeFormatter() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    LaunchedEffect(section) { viewModel.refresh() }
    val title = when (section) {
        AccountSection.CARS -> "로그인된 차량"
        AccountSection.CONSENTS -> "동의 관리"
        AccountSection.BUSINESS -> "사업자 정보"
    }
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            state.notice?.let { NoticeBar(LedgerNotice(it), viewModel::dismissNotice) }
            state.error?.let { ErrorNotice(it, if (state.loadFailed) viewModel::refresh else null) }
            when (section) {
                AccountSection.CARS -> CarSessions(state, viewModel, labels)
                AccountSection.CONSENTS -> Consents(state, viewModel)
                AccountSection.BUSINESS -> BusinessFields(state, viewModel)
            }
        }
        if (section == AccountSection.BUSINESS) {
            Row(Modifier.padding(20.dp)) {
                PrimaryButton("저장", viewModel::saveBusiness, enabled = state.form.canSave, busy = state.isBusy, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CarSessions(state: AccountSettingsState, viewModel: AccountSettingsViewModel, labels: DayTimeFormatter) {
    var revoking by remember { mutableStateOf<CarSession?>(null) }
    if (state.isLoading && state.carSessions.isEmpty()) SkeletonCards(2)
    if (state.carSessions.isEmpty() && !state.isLoading && !state.loadFailed) {
        EmptyState("로그인된 차량이 없어요", "차량 화면의 QR을 찍어 로그인하면 여기에 나와요")
    }
    state.carSessions.forEach { session ->
        Row(Modifier.fillMaxWidth().ruled().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(session.name ?: "이름 없는 차량", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                Text("마지막 사용 ${labels.dayTime(session.lastUsedAt)}", fontSize = 15.sp, color = TextMid)
            }
            TonalButton("로그아웃", { revoking = session }, enabled = !state.isBusy)
        }
    }
    revoking?.let { session ->
        AlertDialog(
            onDismissRequest = { revoking = null },
            title = { Text("이 차량에서 로그아웃할까요?") },
            text = { Text("다시 쓰려면 차량 화면의 QR을 찍어야 해요.", color = TextMid) },
            confirmButton = {
                TextButton(onClick = { revoking = null; viewModel.revoke(session) }) { Text("로그아웃", color = IeoStatus.colors.warning) }
            },
            dismissButton = { TextButton(onClick = { revoking = null }) { Text("취소", color = TextMid) } },
        )
    }
}

@Composable
private fun Consents(state: AccountSettingsState, viewModel: AccountSettingsViewModel) {
    if (state.isLoading && state.consents.isEmpty()) SkeletonCards(4)
    state.consents.forEach { item -> ConsentRow(item) { viewModel.setConsent(item, it) } }
    if (state.consents.any { it.required }) {
        Text("필수 항목을 철회하려면 탈퇴해 주세요", fontSize = 15.sp, color = TextSub, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
    }
}

@Composable
private fun ConsentRow(item: ConsentItem, onChange: (Boolean) -> Unit) {
    val known = Consent.entries.firstOrNull { it.name == item.type }
    Row(Modifier.fillMaxWidth().ruled().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(known?.label ?: item.description, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            known?.hint?.let { Text(it, fontSize = 15.sp, lineHeight = 21.sp, color = TextMid) }
        }
        if (item.required) {
            Text("필수 · 동의함", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMid)
        } else {
            Text(if (item.agreed) "동의" else "안 함", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMid, modifier = Modifier.padding(end = 8.dp))
            Switch(checked = item.agreed, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun BusinessFields(state: AccountSettingsState, viewModel: AccountSettingsViewModel) {
    var clearing by remember { mutableStateOf(false) }
    Text("장부를 세금 신고용으로 내보낼 때 써요. 없으면 비워 두세요", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid, modifier = Modifier.padding(horizontal = 4.dp))
    FormField(state.form.name, viewModel::onBusinessName, "상호")
    FormField(state.form.number, viewModel::onBusinessNumber, "사업자등록번호 (숫자 10자리)", KeyboardType.Number)
    FormField(state.form.representative, viewModel::onRepresentative, "대표자 이름")
    if (state.saved != null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { QuietButton("사업자 정보 지우기", { clearing = true }) }
    }
    if (clearing) {
        AlertDialog(
            onDismissRequest = { clearing = false },
            title = { Text("사업자 정보를 지울까요?") },
            confirmButton = {
                TextButton(onClick = { clearing = false; viewModel.clearBusiness() }) { Text("지우기", color = IeoStatus.colors.warning) }
            },
            dismissButton = { TextButton(onClick = { clearing = false }) { Text("취소", color = TextMid) } },
        )
    }
}
