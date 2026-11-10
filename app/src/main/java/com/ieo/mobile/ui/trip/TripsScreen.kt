package com.ieo.mobile.ui.trip

import com.ieo.mobile.ui.component.ruled
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import com.ieo.mobile.ui.component.ActionRow
import com.ieo.mobile.ui.component.TonalButton
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.ieo.mobile.domain.trip.Trip
import com.ieo.mobile.domain.trip.TripStatus
import com.ieo.mobile.domain.trip.TripStop
import com.ieo.mobile.ui.common.DayTimeFormatter
import com.ieo.mobile.ui.site.SiteInfoLink
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.component.EmptyState
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.QuietButton
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.IeoTag
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TagColor
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

fun tripRoute(trip: Trip) = "${trip.pickup.name} → ${trip.dropoff.name}"

fun tripTime(stop: TripStop, labels: DayTimeFormatter): String = stop.plannedAt?.let(labels::dayTime) ?: "시간 미정"


/** 상태 꼬리표. 색과 글자를 함께 쓴다 */
@Composable
fun TripStatusTag(status: TripStatus) {
    Text(
        status.label,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (status.isInProgress) IeoStatus.colors.driving else TextMid,
    )
}

/** 내 배차 탭 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(viewModel: TripsViewModel, onRegister: () -> Unit, labels: DayTimeFormatter = remember { DayTimeFormatter() }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(White)) {
        Text("내 배차", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
        PullToRefreshBox(isRefreshing = state.isLoading && state.trips.isNotEmpty(), onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (state.loadFailed) state.error?.let { ErrorNotice(it, viewModel::refresh) }
                if (state.isLoading && state.trips.isEmpty()) SkeletonCards(3)
                if (state.trips.isEmpty() && !state.isLoading && !state.loadFailed) {
                    EmptyState("아직 배차가 없어요", "오더 탭에서 요청하거나, 다른 앱에서 받은 배차를 등록해 보세요")
                }
                Section("진행 중", state.inProgress, labels, viewModel)
                // 예정은 모두 같은 상태라 줄마다 상태를 다시 적지 않는다
                Section("예정", state.upcoming, labels, viewModel, showStatus = false)
                Section("지난 운행", state.past, labels, viewModel)
            }
        }
        Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) { PrimaryButton("배차 직접 등록", onRegister, icon = Icons.Outlined.Add) }
    }
}

@Composable
private fun Section(title: String, trips: List<Trip>, labels: DayTimeFormatter, viewModel: TripsViewModel, showStatus: Boolean = true) {
    if (trips.isEmpty()) return
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMid, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
    trips.forEach { TripCard(it, labels, showStatus) { viewModel.open(it.id) } }
}

/** 운행 카드. 홈의 다음 운행에도 쓴다. [showStatus]가 false면 상태 태그 없이 운임만 */
@Composable
fun TripCard(trip: Trip, labels: DayTimeFormatter, showStatus: Boolean = true, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().ruled().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showStatus) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TripStatusTag(trip.status)
                Spacer(Modifier.weight(1f))
                Text(formatWon(trip.fare), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            }
            Text(tripRoute(trip), fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        } else {
            Row(verticalAlignment = Alignment.Top) {
                Text(tripRoute(trip), fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.weight(1f))
                Text(formatWon(trip.fare), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(start = 12.dp))
            }
        }
        Text("상차 ${tripTime(trip.pickup, labels)}", fontSize = 16.sp, color = TextMid)
        Text(listOfNotNull(trip.plateNumber, trip.paymentTypeLabel, trip.brokerName).joinToString(" · "), fontSize = 15.sp, color = TextSub)
    }
}

@Composable
fun TripDetailScreen(trip: Trip, state: TripsState, viewModel: TripsViewModel, labels: DayTimeFormatter = remember { DayTimeFormatter() }, onSite: (Long) -> Unit = {}, onProof: (Long) -> Unit = {}) {
    BackHandler(onBack = viewModel::close)
    var cancelling by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = viewModel::close) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("배차 상세", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Column(Modifier.padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TripStatusTag(trip.status)
                Text(tripRoute(trip), fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            }
            Column(Modifier.fillMaxWidth().ruled().padding(vertical = 18.dp)) {
                Stop("상차", trip.pickup, labels, onSite)
                Spacer(Modifier.height(18.dp))
                Stop("하차", trip.dropoff, labels, onSite)
            }
            Column(Modifier.fillMaxWidth().ruled().padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Line("운임", "${formatWon(trip.fare)} · ${trip.paymentTypeLabel}")
                Line("차량", trip.plateNumber)
                trip.brokerName?.let { Line("주선사", it) }
            }
            if (trip.status.isInProgress || trip.status == TripStatus.COMPLETED) {
                ActionRow(Icons.Outlined.Description, "증빙", "대기 기록 · 인수증") { onProof(trip.id) }
            }
            if (trip.status == TripStatus.SCHEDULED || trip.status.isInProgress) {
                Text("출발과 도착은 차량 앱에서 진행돼요", fontSize = 16.sp, color = TextMid, modifier = Modifier.padding(vertical = 14.dp))
            }
            state.error?.let { ErrorNotice(it) }
            if (trip.canCancel) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { QuietButton("운행 취소", { cancelling = true }) }
        }
    }
    if (cancelling) {
        AlertDialog(
            onDismissRequest = { cancelling = false },
            title = { Text("운행을 취소할까요?") },
            text = { Text("취소하면 되돌릴 수 없어요.", color = TextMid, fontSize = 16.sp) },
            confirmButton = { TextButton(onClick = { cancelling = false; viewModel.cancel() }) { Text("취소하기", color = IeoStatus.colors.warning, fontSize = 16.sp) } },
            dismissButton = { TextButton(onClick = { cancelling = false }) { Text("아니요", color = TextMid, fontSize = 16.sp) } },
        )
    }
}

@Composable
private fun Stop(label: String, stop: TripStop, labels: DayTimeFormatter, onSite: (Long) -> Unit) {
    Column {
        Text(label, fontSize = 15.sp, color = TextSub)
        Text(stop.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        Text(stop.address, fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
        Text(tripTime(stop, labels), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextStrong)
        stop.siteId?.let { id -> SiteInfoLink { onSite(id) } }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 16.sp, color = TextSub, modifier = Modifier.weight(1f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextStrong)
    }
}
