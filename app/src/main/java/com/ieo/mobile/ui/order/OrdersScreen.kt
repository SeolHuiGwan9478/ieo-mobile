package com.ieo.mobile.ui.order

import com.ieo.mobile.ui.site.SiteInfoLink
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.domain.order.DispatchRequest
import com.ieo.mobile.domain.order.Order
import com.ieo.mobile.domain.order.OrderSite
import com.ieo.mobile.domain.order.ReportType
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.component.BrandChip
import com.ieo.mobile.ui.component.EmptyState
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

private val CardShape = RoundedCornerShape(20.dp)
private val LineGray = Color(0xFFE5E8EB)

/** M08 오더 찾기. 근처 오더를 찾으려고 처음에 위치 권한을 묻는다 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(viewModel: OrdersViewModel, texts: OrderTexts = remember { OrderTexts() }, askLocation: Boolean = true, hasVehicle: Boolean = true, onAddVehicle: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }
    LaunchedEffect(Unit) {
        if (askLocation) permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        viewModel.refresh()
    }
    val orders = viewModel.visible(state)

    Column(Modifier.fillMaxSize().background(CanvasGray)) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("오더 찾기", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong, modifier = Modifier.weight(1f))
                Text(
                    "${state.sort.label} ▾",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMid,
                    modifier = Modifier.background(White, RoundedCornerShape(10.dp)).clickable(onClick = viewModel::toggleSort).padding(horizontal = 10.dp, vertical = 7.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandChip("내 위치 50km", state.nearMe && !state.locationMissing, viewModel::toggleNearMe)
                DayFilter.entries.forEach { day -> BrandChip(day.label, state.day == day) { viewModel.selectDay(day) } }
                BrandChip("선불만", state.prepaidOnly, viewModel::togglePrepaid)
            }
            if (state.locationMissing) {
                Text("위치를 몰라 전체 오더를 보여 드려요", fontSize = 15.sp, color = TextMid, modifier = Modifier.padding(top = 10.dp))
            }
        }
        PullToRefreshBox(isRefreshing = state.isLoading && state.orders.isNotEmpty(), onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.loadFailed) state.message?.let { ErrorNotice(it, viewModel::refresh) }
                if (state.isLoading && state.orders.isEmpty()) SkeletonCards(4)
                if (orders.isEmpty() && !state.isLoading && !state.loadFailed) {
                    if (!hasVehicle) {
                        EmptyState("맞는 오더가 없어요", "차를 등록해야 맞는 오더가 보여요", "내 차 등록하기", onAddVehicle)
                    } else if (state.nearMe && state.nearApplied) {
                        EmptyState("근처에 맞는 오더가 없어요", "범위를 넓히면 더 찾을 수 있어요", "전체에서 찾기", viewModel::toggleNearMe)
                    } else {
                        EmptyState("지금 맞는 오더가 없어요", "새 오더가 올라오면 여기에 보여요", "새로고침", viewModel::refresh)
                    }
                }
                orders.forEach { order -> OrderCard(order, texts) { viewModel.open(order.id) } }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderCard(order: Order, texts: OrderTexts, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(White, CardShape).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(texts.route(order), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong, modifier = Modifier.weight(1f))
            Text(formatWon(order.fare), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong)
        }
        Tags(order, texts)
        Column {
            Text("상차 ${texts.pickupTime(order)}", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
            Text("하차 ${texts.dropoffTime(order)}", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
        }
        texts.distance(order)?.let { Text("내 위치에서 상차지까지 $it", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMid) }
        if (order.isRequestedByMe) RequestedBadge()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(order: Order, texts: OrderTexts) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        texts.tags(order).forEach {
            Text(
                it.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = it.color.content,
                modifier = Modifier.background(it.color.background, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun RequestedBadge() {
    Text(
        "요청 중 · 주선사 확인 중",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Orange700,
        modifier = Modifier.background(Orange50, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** M09 오더 상세. 서버에 주선사 전화번호가 없어 시안의 전화 버튼 자리에 신고를 둔다 */
@Composable
fun OrderDetailScreen(order: Order, state: OrdersState, viewModel: OrdersViewModel, texts: OrderTexts = remember { OrderTexts() }, onSite: (Long) -> Unit = {}) {
    BackHandler(onBack = viewModel::close)
    Column(Modifier.fillMaxSize().background(CanvasGray).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = viewModel::close) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("오더 상세", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(texts.route(order), fontSize = 23.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong)
                Tags(order, texts)
            }
            FareCard(order)
            RouteCard(order, texts, onSite)
            BrokerCard(order, texts)
            order.myRequest?.takeIf { order.isRequestedByMe }?.let { RequestCountdown(it) }
            state.message?.let { if (state.messageIsError) ErrorNotice(it) else Text(it, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, color = TextMid, modifier = Modifier.padding(horizontal = 4.dp)) }
        }
        Row(Modifier.fillMaxWidth().background(White).padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TonalButton("신고", viewModel::startReport, modifier = Modifier.weight(1f))
            if (order.isRequestedByMe) {
                PrimaryButton("요청 철회", viewModel::withdraw, busy = state.isBusy, modifier = Modifier.weight(2.2f))
            } else {
                PrimaryButton("배차 요청", viewModel::request, busy = state.isBusy, modifier = Modifier.weight(2.2f))
            }
        }
    }
    if (state.reporting) ReportDialog(state.isBusy, viewModel)
}

private val FareGradient = Brush.linearGradient(listOf(Color(0xFFFF8A5B), Color(0xFFFF6C37), Color(0xFFF25A22)))

@Composable
private fun FareCard(order: Order) {
    Column(Modifier.fillMaxWidth().background(FareGradient, CardShape).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("운임", fontSize = 16.sp, color = White.copy(alpha = 0.9f))
        Text(formatWon(order.fare), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = White)
        Text("${order.paymentTypeLabel} · ${order.expectedPaymentDays}일 안에 지급", fontSize = 15.sp, color = White.copy(alpha = 0.9f))
    }
}

@Composable
private fun RouteCard(order: Order, texts: OrderTexts, onSite: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().background(White, CardShape).padding(horizontal = 20.dp, vertical = 18.dp)) {
        val distance = texts.distance(order)
        Stop("상차" + (distance?.let { " · 내 위치에서 $it" } ?: ""), order.pickup, texts.pickupTime(order), Orange600, onSite)
        Spacer(Modifier.height(18.dp))
        Stop("하차", order.dropoff, texts.dropoffTime(order), TextStrong, onSite)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(CanvasGray))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("화물", fontSize = 16.sp, color = TextSub)
            Text(texts.cargo(order), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stop(label: String, site: OrderSite, time: String, dot: Color, onSite: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.padding(top = 5.dp).size(10.dp).background(dot, CircleShape))
        Column {
            Text(label, fontSize = 15.sp, color = TextSub)
            Text(site.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            Text(site.address, fontSize = 15.sp, color = TextMid)
            Text(time, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Orange700)
            site.siteId?.let { id -> SiteInfoLink { onSite(id) } }
        }
    }
}

@Composable
private fun BrokerCard(order: Order, texts: OrderTexts) {
    Column(Modifier.fillMaxWidth().background(White, CardShape).padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text(order.brokerName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        order.brokerMetrics?.let { Text(texts.metrics(it), fontSize = 15.sp, lineHeight = 19.sp, color = TextSub) }
    }
}

/** 10분 안에 확정되지 않으면 요청이 만료된다 (DSP-023) */
@Composable
private fun RequestCountdown(request: DispatchRequest) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(request.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val left = Duration.between(Instant.ofEpochMilli(now), request.expiresAt).seconds.coerceAtLeast(0)
    Row(
        Modifier.fillMaxWidth().background(Orange50, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("주선사 확인을 기다려요", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Orange700, modifier = Modifier.weight(1f))
        Text("%d:%02d".format(left / 60, left % 60), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Orange700)
    }
}

@Composable
private fun ReportDialog(busy: Boolean, viewModel: OrdersViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::cancelReport,
        title = { Text("무엇이 문제인가요?") },
        text = {
            Column {
                ReportType.entries.forEach { type ->
                    Text(
                        type.label,
                        fontSize = 16.sp,
                        color = TextStrong,
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { viewModel.report(type) }.padding(vertical = 15.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = viewModel::cancelReport) { Text("취소", color = TextMid) } },
    )
}
