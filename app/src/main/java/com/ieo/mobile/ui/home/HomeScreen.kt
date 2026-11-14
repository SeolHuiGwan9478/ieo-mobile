package com.ieo.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddRoad
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.domain.document.DriverDocuments
import com.ieo.mobile.domain.trip.Trip
import com.ieo.mobile.ui.common.DayTimeFormatter
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.component.IeoSymbol
import com.ieo.mobile.ui.document.VerifiedBadge
import com.ieo.mobile.ui.notification.NotificationBell
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.DrivingPale
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.IeoTag
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange500
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TagColor
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.WarningPale
import com.ieo.mobile.ui.theme.White
import com.ieo.mobile.ui.trip.TripStatusTag
import com.ieo.mobile.ui.trip.tripRoute
import com.ieo.mobile.ui.trip.tripTime

/**
 * 홈 (M07). 자주 하는 일을 사각형 버튼으로 바로 누른다.
 * 맨 위 큰 버튼 두 개(오더 찾기, 차량에 로그인), 다가오는 운행 한 장, 작은 버튼 여덟 개, 이번 달 돈 순서
 */
@Composable
fun HomeScreen(
    name: String?,
    nextTrip: Trip?,
    monthNet: Long?,
    owed: Long,
    invitationCount: Int,
    onLinks: () -> Unit,
    onEarnings: () -> Unit,
    onScan: () -> Unit,
    onVehicles: () -> Unit,
    onOpenTrip: (Trip) -> Unit,
    onFindOrders: () -> Unit,
    unreadNotifications: Int = 0,
    onNotifications: () -> Unit = {},
    documents: DriverDocuments? = null,
    onDocuments: () -> Unit = {},
    openOrderCount: Int = 0,
    onTrips: () -> Unit = {},
    onRegisterTrip: () -> Unit = {},
    onReceivables: () -> Unit = {},
    recommendationLocked: Boolean = false,
    onRecommendation: () -> Unit = {},
) {
    val labels = remember { DayTimeFormatter() }
    Column(
        Modifier.fillMaxSize().background(CanvasGray).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.padding(start = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IeoSymbol(size = 32.dp)
            // 이름이 길면 이름을 줄이고(…) 인증 표시와 알림 종은 그대로 둔다
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name?.takeIf { it.isNotBlank() }?.let { "${it}님" } ?: "이에오", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextStrong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (documents?.verified == true) VerifiedBadge()
            }
            NotificationBell(unreadNotifications, onNotifications)
        }
        documents?.takeIf { !it.verified }?.let { DocumentsCard(it, onDocuments) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton(
                title = "오더\n찾기",
                detail = "내 차에 맞는 오더",
                chip = openOrderCount.takeIf { it > 0 }?.let { "모집 중 ${it}건" },
                icon = Icons.Outlined.Search,
                background = Orange600,
                onClick = onFindOrders,
                modifier = Modifier.weight(1f),
            )
            BigButton(
                title = "차량에\n로그인",
                detail = "차량 화면 QR 찍기",
                chip = null,
                icon = Icons.Outlined.QrCodeScanner,
                background = TextStrong,
                onClick = onScan,
                modifier = Modifier.weight(1f),
            )
        }
        if (nextTrip != null) {
            NextTripCard(nextTrip, labels) { onOpenTrip(nextTrip) }
        } else {
            Card(onTrips) {
                Text("오늘 잡힌 배차가 없어요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                Text("오더를 찾거나 직접 등록해 보세요", fontSize = 15.sp, color = TextMid)
            }
        }
        val tiles = listOf(
            Tile("내 배차", Icons.Outlined.CalendarMonth, TagColor(DrivingPale, DrivingInk), onClick = onTrips),
            Tile("운행 등록", Icons.Outlined.AddRoad, IeoTag.Navy, onClick = onRegisterTrip),
            Tile("장부", Icons.Outlined.Book, IeoTag.Teal, onClick = onEarnings),
            Tile("미수금", Icons.Outlined.Payments, TagColor(WarningPale, IeoStatus.colors.warning), dot = owed > 0, onClick = onReceivables),
            Tile("단골", Icons.Outlined.Handshake, IeoTag.Plum, dot = invitationCount > 0, onClick = onLinks),
            Tile("배차 추천", Icons.Outlined.AutoAwesome, IeoTag.Mine, badge = "PRO".takeIf { recommendationLocked }, onClick = onRecommendation),
            Tile("서류", Icons.Outlined.Description, IeoTag.Sand, dot = documents?.verified == false, onClick = onDocuments),
            Tile("내 차량", Icons.Outlined.LocalShipping, IeoTag.Gray, onClick = onVehicles),
        )
        tiles.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { SmallButton(it, Modifier.weight(1f)) }
            }
        }
        if (monthNet != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyCard("이번 달 실수익", formatWon(monthNet), TextStrong, onEarnings, Modifier.weight(1f))
                MoneyCard("못 받은 돈", formatWon(owed), if (owed > 0) IeoStatus.colors.warning else TextStrong, onReceivables, Modifier.weight(1f))
            }
        }
    }
}

private data class Tile(
    val label: String,
    val icon: ImageVector,
    val color: TagColor,
    val dot: Boolean = false,
    val badge: String? = null,
    val onClick: () -> Unit,
)

private val CardShape = RoundedCornerShape(16.dp)

/** 운행 꼬리표 바탕(DrivingPale) 위 진한 파랑 */
private val DrivingInk = Color(0xFF1D4ED8)

/** 큰 버튼. 모집 건수까지 들어가게 세로로 조금 길다. 바탕색 위에 흰 글자 */
@Composable
private fun BigButton(
    title: String,
    detail: String,
    chip: String?,
    icon: ImageVector,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.aspectRatio(0.85f).clip(RoundedCornerShape(20.dp)).background(background).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (chip != null) {
            Text(
                chip,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.clip(CircleShape).background(White.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        } else {
            Spacer(Modifier.height(1.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = White, modifier = Modifier.size(34.dp))
            Text(title, fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, color = White)
            Text(detail, fontSize = 14.sp, color = White.copy(alpha = 0.85f))
        }
    }
}

/** 정사각형 작은 버튼. 할 일이 있으면 오른쪽 위에 점, 프로 기능이면 PRO 표시 */
@Composable
private fun SmallButton(tile: Tile, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1f).clip(CardShape).background(White).clickable(onClick = tile.onClick)) {
        Column(
            Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tile.color.background), contentAlignment = Alignment.Center) {
                Icon(tile.icon, contentDescription = null, tint = tile.color.content, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(tile.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextStrong, textAlign = TextAlign.Center, maxLines = 1)
        }
        when {
            tile.badge != null -> Text(
                tile.badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Orange700,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Orange50).padding(horizontal = 5.dp, vertical = 1.dp),
            )
            tile.dot -> Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(8.dp).clip(CircleShape).background(Orange500))
        }
    }
}

@Composable
private fun NextTripCard(trip: Trip, labels: DayTimeFormatter, onClick: () -> Unit) {
    Card(onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TripStatusTag(trip.status)
            Spacer(Modifier.weight(1f))
            Text(formatWon(trip.fare), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Text(tripRoute(trip), fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        Text(
            listOfNotNull("상차 ${tripTime(trip.pickup, labels)}", trip.brokerName).joinToString(" · "),
            fontSize = 15.sp,
            color = TextMid,
        )
    }
}

@Composable
private fun MoneyCard(label: String, amount: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clip(CardShape).background(White).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(label, fontSize = 14.sp, color = TextMid)
        Text(amount, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1)
    }
}

/** 서류가 다 승인되기 전까지 맨 위에 둔다. 승인되면 사라진다 */
@Composable
private fun DocumentsCard(documents: DriverDocuments, onClick: () -> Unit) {
    val (title, detail) = when {
        documents.rejectedCount > 0 -> "반려된 서류가 ${documents.rejectedCount}건 있어요" to "사유를 보고 다시 올려 주세요"
        documents.missingCount > 0 -> "서류 ${documents.missingCount}가지를 올려 주세요" to "승인되면 오더에 배차를 요청할 수 있어요"
        else -> "서류를 심사하고 있어요" to "승인되면 바로 배차를 요청할 수 있어요"
    }
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(Orange50).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Orange700)
            Text(detail, fontSize = 15.sp, color = TextMid)
        }
        Text("${documents.approvedRequired}/${documents.requiredTotal}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        Text("  ›", fontSize = 20.sp, color = TextMid)
    }
}

@Composable
private fun Card(onClick: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(CardShape).background(White).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}
