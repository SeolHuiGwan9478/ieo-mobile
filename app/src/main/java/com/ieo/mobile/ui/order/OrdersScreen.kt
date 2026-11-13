package com.ieo.mobile.ui.order

import com.ieo.mobile.ui.component.ruled
import com.ieo.mobile.ui.site.SiteInfoLink
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import com.ieo.mobile.domain.order.CargoCategory
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import com.ieo.mobile.R
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.ieo.mobile.ui.common.roundedToManwon
import com.ieo.mobile.domain.order.OrderProfit
import com.ieo.mobile.domain.order.OrderSite
import com.ieo.mobile.domain.order.ReportType
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.component.EmptyState
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.component.TonalButton
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

private val LineGray = Color(0xFFE5E8EB)

/**
 * M08 오더 찾기. 근처 오더를 찾으려고 처음에 위치 권한을 묻는다.
 * 위에는 새로고침과 "조건" 버튼, 걸어 둔 조건, 마지막 업데이트 시간. 목록은 화면이 보이는 동안 1분마다 다시 받는다
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(viewModel: OrdersViewModel, texts: OrderTexts = remember { OrderTexts() }, askLocation: Boolean = true, hasVehicle: Boolean = true, onAddVehicle: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }
    LaunchedEffect(Unit) {
        if (askLocation) permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        viewModel.refresh()
    }
    // 화면이 보이는 동안만 1분마다 다시 받는다. 앱을 내리면 멈춘다
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(OrdersViewModel.AUTO_REFRESH_MILLIS)
                viewModel.autoRefresh()
            }
        }
    }
    val orders = viewModel.visible(state)

    Column(Modifier.fillMaxSize().background(White)) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("오더 찾기", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.weight(1f))
                ToolButton(onClick = viewModel::refresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "새로고침", tint = TextStrong, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                ToolButton(onClick = viewModel::openConditions) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = TextStrong, modifier = Modifier.size(18.dp))
                    Text("조건", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(start = 5.dp))
                    if (state.conditions.changedCount > 0) {
                        Box(Modifier.padding(start = 6.dp).size(20.dp).background(Orange600, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${state.conditions.changedCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = White)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    state.conditions.summary.forEach { ConditionPill(it) }
                }
                state.updatedAt?.let { UpdatedLabel(it) }
            }
            if (state.locationMissing) {
                Text("위치를 몰라 전국 오더를 보여 드려요", fontSize = 15.sp, color = TextMid, modifier = Modifier.padding(top = 8.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.isLoading && state.orders.isEmpty()) "" else "${orders.size}건", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSub, modifier = Modifier.weight(1f))
                Text(
                    "${state.sort.label} ▾",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMid,
                    modifier = Modifier.clickable(onClick = viewModel::toggleSort).padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }
        PullToRefreshBox(isRefreshing = state.isLoading && state.orders.isNotEmpty(), onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (state.loadFailed) state.message?.let { ErrorNotice(it, viewModel::refresh) }
                if (state.isLoading && state.orders.isEmpty()) SkeletonCards(4)
                if (orders.isEmpty() && !state.isLoading && !state.loadFailed) {
                    if (!hasVehicle) {
                        EmptyState("맞는 오더가 없어요", "차를 등록해야 맞는 오더가 보여요", "내 차 등록하기", onAddVehicle)
                    } else if (state.conditions.changedCount > 0) {
                        EmptyState("조건에 맞는 오더가 없어요", "조건을 넓히면 더 찾을 수 있어요", "조건 바꾸기", viewModel::openConditions)
                    } else {
                        EmptyState("지금 맞는 오더가 없어요", "새 오더가 올라오면 여기에 보여요", "새로고침", viewModel::refresh)
                    }
                }
                orders.forEach { order -> OrderRow(order, texts, isNew = order.id in state.newIds) { viewModel.open(order.id) } }
            }
        }
    }
    state.draft?.let { draft -> ConditionsSheet(draft, state.draftCount, viewModel) }
}

@Composable
private fun ToolButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier.heightIn(min = 40.dp).background(CanvasGray, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun ConditionPill(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Orange700,
        modifier = Modifier.background(Orange50, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** "방금 업데이트", "3분 전 업데이트". 30초마다 다시 센다 */
@Composable
private fun UpdatedLabel(updatedAt: Instant) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(updatedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }
    Text(updatedLabel(updatedAt, Instant.ofEpochMilli(now)), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSub, modifier = Modifier.padding(start = 8.dp))
}

internal fun updatedLabel(updatedAt: Instant, now: Instant): String {
    val minutes = Duration.between(updatedAt, now).toMinutes()
    return if (minutes < 1) "방금 업데이트" else "${minutes}분 전 업데이트"
}

/** 한 건: 왼쪽은 상차→하차를 점과 선으로, 오른쪽은 운임·실수익·거리, 아래는 태그 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderRow(order: Order, texts: OrderTexts, isNew: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().ruled().clickable(onClick = onClick).padding(vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Row(Modifier.weight(1f).height(IntrinsicSize.Min)) {
                // 위 화살표 핀은 싣는 곳, 아래 화살표 핀은 내리는 곳 (차량 앱과 같다)
                Column(Modifier.width(18.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    PinIcon(R.drawable.ic_pin_pickup, Modifier.padding(top = 2.dp).size(18.dp))
                    Box(Modifier.padding(vertical = 2.dp).width(2.dp).weight(1f).background(LineGray))
                    PinIcon(R.drawable.ic_pin_dropoff, Modifier.padding(bottom = 22.dp).size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(order.pickup.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                        if (isNew) {
                            Text(
                                "새로 올라옴",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = White,
                                modifier = Modifier.padding(start = 6.dp).background(Orange600, RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Text(texts.pickupTime(order), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSub, modifier = Modifier.padding(top = 1.dp, bottom = 8.dp))
                    Text(order.dropoff.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                    Text(texts.dropoffTime(order), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSub, modifier = Modifier.padding(top = 1.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                Text(formatWon(order.fare), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                texts.netProfit(order)?.let { Text(it, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Orange700, modifier = Modifier.padding(top = 2.dp)) }
                texts.distance(order)?.let { Text("상차지까지 $it", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSub, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            texts.tags(order).forEach { TagChip(it) }
        }
        if (order.isRequestedByMe) Box(Modifier.padding(top = 8.dp)) { RequestedBadge() }
    }
}

/** 아래에서 올라오는 조건 창. 고른 뒤 아래 버튼을 눌러야 목록이 바뀐다 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ConditionsSheet(draft: OrderConditions, count: Int?, viewModel: OrdersViewModel) {
    // 반만 올라오면 아래 버튼이 가려져서 처음부터 끝까지 올린다
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = viewModel::closeConditions, sheetState = sheetState, containerColor = White) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            // 초기화는 아래 버튼 하나만 둔다
            Text("조건 설정", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextStrong, modifier = Modifier.padding(vertical = 8.dp))
            ConditionGroup("내 주변 거리") {
                OrderRadius.entries.forEach { r -> ConditionChip(r.label, draft.radius == r) { viewModel.editDraft { it.copy(radius = r) } } }
            }
            ConditionGroup("상차 날짜") {
                DayFilter.entries.forEach { d -> ConditionChip(d.label, draft.day == d) { viewModel.editDraft { it.copy(day = d) } } }
            }
            ConditionGroup("최소 운임") {
                MinFare.entries.forEach { f -> ConditionChip(f.label, draft.minFare == f) { viewModel.editDraft { it.copy(minFare = f) } } }
            }
            ConditionGroup("결제 방식") {
                PaymentFilter.entries.forEach { p -> ConditionChip(p.label, draft.payment == p) { viewModel.editDraft { it.copy(payment = p) } } }
            }
            ConditionGroup("제외할 화물", last = true, note = "배차 추천에도 똑같이 적용돼요") {
                CargoCategory.entries.filter { it.excludable }.forEach { c ->
                    ConditionChip(c.label, c in draft.excluded) {
                        viewModel.editDraft { it.copy(excluded = if (c in it.excluded) it.excluded - c else it.excluded + c) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TonalButton("초기화", viewModel::resetDraft, modifier = Modifier.weight(1f))
                PrimaryButton(
                    when (count) {
                        null -> "오더 보기"
                        0 -> "맞는 오더가 없어요"
                        else -> "오더 ${count}건 보기"
                    },
                    viewModel::applyConditions,
                    modifier = Modifier.weight(2f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionGroup(title: String, last: Boolean = false, note: String? = null, chips: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().then(if (last) Modifier else Modifier.ruled()).padding(vertical = 14.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMid, modifier = Modifier.padding(bottom = if (note == null) 10.dp else 2.dp))
        note?.let { Text(it, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSub, modifier = Modifier.padding(bottom = 10.dp)) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { chips() }
    }
}

/** 고른 칸은 연한 주황 바탕에 주황 테두리. 아래 진한 주황 버튼과 겹치지 않게 연하게 둔다 */
@Composable
private fun ConditionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .background(if (selected) Orange50 else CanvasGray, shape)
            .then(if (selected) Modifier.border(1.5.dp, Orange600, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(text, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Orange700 else TextMid)
    }
}

/** 상세 위 태그를 글자 한 줄로 쓴다 */
@Composable
private fun Tags(order: Order, texts: OrderTexts) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        texts.detailTags(order).forEach { TagChip(it, fontSize = 14.sp) }
    }
}

/** 목록·상세 태그 한 칸 (차량 앱 KeywordTag와 같은 색) */
@Composable
private fun TagChip(tag: OrderTag, fontSize: TextUnit = 13.sp) {
    Text(
        tag.text,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        color = tag.color.content,
        modifier = Modifier.background(tag.color.background, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
private fun PinIcon(@DrawableRes pin: Int, modifier: Modifier) {
    Icon(painterResource(pin), contentDescription = null, tint = Color.Unspecified, modifier = modifier)
}

@Composable
private fun RequestedBadge() {
    Text(
        "요청 중 · 주선사 확인 중",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Orange700,
    )
}

/** M09 오더 상세. 서버에 주선사 전화번호가 없어 시안의 전화 버튼 자리에 신고를 둔다 */
@Composable
fun OrderDetailScreen(order: Order, state: OrdersState, viewModel: OrdersViewModel, texts: OrderTexts = remember { OrderTexts() }, onSite: (Long) -> Unit = {}, verified: Boolean = true, onDocuments: () -> Unit = {}) {
    BackHandler(onBack = viewModel::close)
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = viewModel::close) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("오더 상세", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Column(Modifier.padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(texts.route(order), fontSize = 23.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                Tags(order, texts)
            }
            FareCard(order)
            ProfitCard(order, state.profit, texts)
            RouteCard(order, texts, onSite)
            BrokerCard(order, texts)
            order.myRequest?.takeIf { order.isRequestedByMe }?.let { RequestCountdown(it) }
            if (!verified && !order.isRequestedByMe) {
                Text("서류 4가지가 승인되면 배차를 요청할 수 있어요", fontSize = 16.sp, lineHeight = 23.sp, color = TextMid, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            }
        }
        // 누른 버튼 바로 위에 결과를 띄운다. 잘 됐다는 안내는 3초 뒤 사라지고, 실패는 남는다
        state.message?.let { ActionMessage(it, state.messageIsError, viewModel::clearMessage) }
        Row(Modifier.fillMaxWidth().background(White).padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TonalButton(if (order.reportedByMe) "신고함" else "신고", viewModel::startReport, enabled = !order.reportedByMe, modifier = Modifier.weight(1f))
            if (order.isRequestedByMe) {
                PrimaryButton("요청 철회", viewModel::withdraw, busy = state.isBusy, modifier = Modifier.weight(2.2f))
            } else if (!verified) {
                PrimaryButton("서류 올리기", onDocuments, modifier = Modifier.weight(2.2f))
            } else {
                PrimaryButton("배차 요청", viewModel::request, busy = state.isBusy, modifier = Modifier.weight(2.2f))
            }
        }
    }
    if (state.reporting) ReportSheet(state.isBusy, viewModel)
}

@Composable
private fun ActionMessage(message: String, isError: Boolean, onTimeout: () -> Unit) {
    if (!isError) {
        LaunchedEffect(message) {
            delay(3_000)
            onTimeout()
        }
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 10.dp)) {
        Text(
            message,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) IeoStatus.colors.warning else TextStrong,
            modifier = Modifier.fillMaxWidth()
                .background(if (isError) Orange50 else CanvasGray, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun FareCard(order: Order) {
    Column(Modifier.fillMaxWidth().ruled().padding(vertical = 18.dp)) {
        Text("운임", fontSize = 16.sp, color = TextMid)
        Text(formatWon(order.fare), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        Text("${order.paymentTypeLabel} · ${order.expectedPaymentDays}일 안에 지급", fontSize = 15.sp, color = TextMid)
    }
}

/** 위치를 알 때만 보인다. 내역을 받기 전에는 목록의 어림값을 보여 준다 */
@Composable
private fun ProfitCard(order: Order, profit: OrderProfit?, texts: OrderTexts) {
    val net = profit?.netProfit ?: order.netProfit ?: return
    val note = when {
        profit == null -> "기름값만 뺀 어림값"
        profit.tollFare == null -> "기름값만 뺀 금액"
        else -> "기름값·통행료 뺀 금액"
    }
    Column(Modifier.fillMaxWidth().ruled().padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("실수익 · $note", fontSize = 16.sp, color = TextMid)
        Text(if (profit == null) "약 ${formatWon(roundedToManwon(net))}" else formatWon(net), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Orange700)
        profit?.let {
            it.tollFare?.let { toll -> ProfitLine("예상 통행료", formatWon(toll)) }
            ProfitLine("기름값", formatWon(it.fuelCost))
            ProfitLine("상차지까지", it.emptyRunLabel)
        }
        // 실제 경로를 받기 전에는 목록과 같은 어림 거리
        if (profit == null) texts.distance(order)?.let { ProfitLine("상차지까지", it) }
        order.routeAverageFare?.let { ProfitLine("이 노선 평균 운임", "${formatWon(roundedToManwon(it))} (Ieo 완료 운행 기준)") }
    }
}

@Composable
private fun ProfitLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 16.sp, color = TextMid, modifier = Modifier.weight(1f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
    }
}

@Composable
private fun RouteCard(order: Order, texts: OrderTexts, onSite: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().ruled().padding(vertical = 18.dp)) {
        Stop("상차", order.pickup, texts.pickupTime(order), R.drawable.ic_pin_pickup, onSite, order.limited)
        Spacer(Modifier.height(18.dp))
        Stop("하차", order.dropoff, texts.dropoffTime(order), R.drawable.ic_pin_dropoff, onSite, order.limited)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(CanvasGray))
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Text("화물", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextSub)
            Text(texts.cargo(order), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            texts.loadingTag(order)?.let { TagChip(it, fontSize = 14.sp) }
        }
    }
}

@Composable
private fun Stop(label: String, site: OrderSite, time: String, @DrawableRes pin: Int, onSite: (Long) -> Unit, limited: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PinIcon(pin, Modifier.size(22.dp))
        Column {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextSub)
            Text(site.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            if (limited) {
                Text("정확한 주소는 서류 승인 후 보여요", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextSub)
            } else {
                Text(site.address, fontSize = 15.sp, color = TextMid)
            }
            Text(time, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextStrong)
            site.siteId?.let { id -> SiteInfoLink { onSite(id) } }
        }
    }
}

@Composable
private fun BrokerCard(order: Order, texts: OrderTexts) {
    Column(Modifier.fillMaxWidth().ruled().padding(vertical = 14.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            if (order.limited) {
                Text("주선사는 서류 승인 후 보여요", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextSub)
            } else {
                Text(order.brokerName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            }
            order.brokerMetrics?.let { TagChip(texts.brokerTag(it), fontSize = 14.sp) }
        }
        // 기록이 적으면 이름 옆 태그로 충분하다
        order.brokerMetrics?.takeIf { it.published }?.let { Text(texts.metrics(it), fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, color = TextSub) }
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
        Modifier.fillMaxWidth().ruled().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("주선사 확인을 기다려요", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Orange700, modifier = Modifier.weight(1f))
        Text("%d:%02d".format(left / 60, left % 60), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Orange700)
    }
}

/** 신고 사유를 고르는 창. 고르면 바로 접수한다 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(busy: Boolean, viewModel: OrdersViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = viewModel::cancelReport, sheetState = sheetState, containerColor = White) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Text("무엇이 문제인가요?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            Text("운영팀이 확인해요. 신고는 오더마다 한 번 할 수 있어요", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextSub, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
            ReportType.entries.forEachIndexed { index, type ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(CanvasGray))
                Text(
                    type.label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextStrong,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { viewModel.report(type) }.padding(vertical = 17.dp),
                )
            }
            TonalButton("취소", viewModel::cancelReport, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
    }
}
