package com.ieo.mobile.ui.trip

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.domain.trip.PaymentType
import com.ieo.mobile.domain.trip.StopChoice
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.component.BrandChip
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.FormField
import com.ieo.mobile.ui.component.Heading
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.StepLabel
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

private val LineGray = Color(0xFFD1D6DB)

/** 다른 앱에서 받은 배차 등록. 한 화면에 하나씩 묻는다 */
@Composable
fun RegisterTripScreen(viewModel: RegisterTripViewModel, onClose: () -> Unit, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    BackHandler { if (!viewModel.back()) onClose() }
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding().imePadding()) {
        IconButton(onClick = { if (!viewModel.back()) onClose() }, modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong)
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
            when (state.step) {
                RegisterStep.PICKUP -> PlaceStep("1 / 4 · 상차지", "어디서 싣나요?", state, viewModel, showHere = true)
                RegisterStep.DROPOFF -> PlaceStep("2 / 4 · 하차지", "어디로 가나요?", state, viewModel, showHere = false)
                RegisterStep.WHEN -> WhenStep(state, viewModel)
                RegisterStep.MONEY -> MoneyStep(state, viewModel)
            }
        }
    }
}

@Composable
private fun ColumnScope.PlaceStep(step: String, title: String, state: RegisterState, viewModel: RegisterTripViewModel, showHere: Boolean) {
    StepLabel(step)
    Heading(title)
    Spacer(Modifier.height(18.dp))
    FormField(state.query, viewModel::onQuery, "주소 입력 (예: 평택시 포승읍)")
    Spacer(Modifier.height(10.dp))
    PrimaryButton("주소 찾기", viewModel::search, enabled = state.query.isNotBlank(), busy = state.isSearching)
    state.error?.let {
        Spacer(Modifier.height(10.dp))
        ErrorNotice(it)
    }
    Spacer(Modifier.height(14.dp))
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.searched && state.results.isEmpty()) {
            Text("찾지 못했어요. 시·군·구부터 적어 보세요", fontSize = 16.sp, color = TextMid)
        }
        state.results.forEach { PlaceRow(it.name, it.address) { viewModel.choose(it) } }
        if (showHere) state.here?.let { PlaceRow("지금 여기", it.address) { viewModel.choose(it) } }
        if (state.visited.isNotEmpty()) {
            Text("다녀온 곳", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMid, modifier = Modifier.padding(top = 6.dp))
            state.visited.forEach { PlaceRow(it.name, it.address) { viewModel.choose(it as StopChoice) } }
        }
    }
}

@Composable
private fun PlaceRow(title: String, address: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).border(1.5.dp, LineGray, shape).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        Text(address, fontSize = 15.sp, color = TextSub)
    }
}

@Composable
private fun ColumnScope.WhenStep(state: RegisterState, viewModel: RegisterTripViewModel) {
    StepLabel("3 / 4 · 상차 시각")
    Heading("언제 싣나요?")
    Spacer(Modifier.height(22.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PlannedDay.entries.forEach { day -> BrandChip(day.label, state.day == day) { viewModel.selectDay(day) } }
    }
    if (state.day != PlannedDay.UNKNOWN) {
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TonalButton("−30분", { viewModel.shiftTime(-30) }, modifier = Modifier.width(100.dp))
            Text("%02d:%02d".format(state.time.hour, state.time.minute), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            TonalButton("+30분", { viewModel.shiftTime(30) }, modifier = Modifier.width(100.dp))
        }
    }
    Spacer(Modifier.weight(1f))
    PrimaryButton("다음", viewModel::whenDone)
    Spacer(Modifier.height(12.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.MoneyStep(state: RegisterState, viewModel: RegisterTripViewModel) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        StepLabel("4 / 4 · 운임")
        Heading("운임이 얼마예요?")
        Spacer(Modifier.height(4.dp))
        Text("${state.pickup?.name.orEmpty()} → ${state.dropoff?.name.orEmpty()}", fontSize = 16.sp, color = TextMid)
        Spacer(Modifier.height(18.dp))
        FormField(state.fareManwon, viewModel::onFare, "운임 (만 원)", KeyboardType.Number)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1L, 5L, 10L).forEach { BrandChip("+${it}만", false) { viewModel.addFare(it) } }
        }
        if (state.fare > 0) Text(formatWon(state.fare), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(20.dp))
        Text("결제", fontSize = 16.sp, color = TextSub, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentType.entries.forEach { type -> BrandChip(type.label, state.paymentType == type) { viewModel.selectPayment(type) } }
        }
        Spacer(Modifier.height(20.dp))
        FormField(state.brokerName, viewModel::onBroker, "주선사 (없으면 비워 두세요)")
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            ErrorNotice(it)
        }
    }
    Spacer(Modifier.height(8.dp))
    PrimaryButton("등록하기", viewModel::submit, enabled = state.fare > 0, busy = state.isBusy)
    Spacer(Modifier.height(12.dp))
}
