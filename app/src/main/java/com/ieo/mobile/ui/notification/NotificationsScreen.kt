package com.ieo.mobile.ui.notification

import com.ieo.mobile.ui.component.ruled
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Handshake
import com.ieo.mobile.ui.component.IconBadge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ieo.mobile.domain.notification.AppNotification
import com.ieo.mobile.domain.notification.NotificationType
import com.ieo.mobile.ui.component.EmptyState
import com.ieo.mobile.ui.component.ErrorNotice
import com.ieo.mobile.ui.component.SkeletonCards
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private const val BANNER_MILLIS = 6_000L
private val DATE = DateTimeFormatter.ofPattern("M월 d일")

/** 시각을 "방금", "5분 전"처럼 쉬운 말로 */
fun timeAgo(at: Instant, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String {
    val minutes = Duration.between(at, now).toMinutes()
    return when {
        minutes < 1 -> "방금"
        minutes < 60 -> "${minutes}분 전"
        minutes < 24 * 60 -> "${minutes / 60}시간 전"
        else -> DATE.format(at.atZone(zone))
    }
}

@Composable
private fun typeColor(type: NotificationType): Color =
    if (type == NotificationType.DISPATCH_CONFIRMED) IeoStatus.colors.done else TextMid

private fun typeIcon(type: NotificationType) = when (type) {
    NotificationType.DISPATCH_CONFIRMED -> Icons.Outlined.CheckCircle
    NotificationType.DISPATCH_DECLINED -> Icons.Outlined.Block
    NotificationType.DISPATCH_EXPIRED -> Icons.Outlined.Schedule
    NotificationType.DRIVER_LINK_REQUESTED -> Icons.Outlined.Handshake
}

@Composable
private fun TypeBadge(type: NotificationType) =
    IconBadge(typeIcon(type), tint = typeColor(type), background = if (type == NotificationType.DISPATCH_CONFIRMED) Color(0xFFE6F4EA) else CanvasGray, size = 38.dp)

/** 홈 머리의 종. 숫자는 9+까지만 */
@Composable
fun NotificationBell(unreadCount: Int, onClick: () -> Unit) {
    val description = if (unreadCount > 0) "알림, 새 알림 ${unreadCount}건" else "알림"
    Box(Modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        IconButton(onClick = onClick) {
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = TextStrong, modifier = Modifier.size(28.dp))
        }
        if (unreadCount > 0) {
            Box(Modifier.align(Alignment.TopEnd).background(Orange700, CircleShape).padding(horizontal = 6.dp, vertical = 1.dp)) {
                Text(if (unreadCount > 9) "9+" else "$unreadCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White)
            }
        }
    }
}

/** 알림함. 열면 모두 읽은 것으로 치고, 이번에 처음 보는 줄만 표시한다 */
@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel, onOpen: (AppNotification) -> Unit, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(White).safeDrawingPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로", tint = TextStrong) }
            Text("알림", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            state.error?.let { ErrorNotice(it, viewModel::load) }
            if (state.isLoading && state.items.isEmpty()) SkeletonCards(3)
            if (!state.isLoading && state.error == null && state.items.isEmpty()) {
                EmptyState("아직 알림이 없어요", "배차가 확정되거나 마감되면\n여기서 알려 드려요")
            }
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.items, key = { it.id }) { item -> NotificationRow(item, state.isNew(item)) { onOpen(item) } }
        }
    }
}

@Composable
private fun NotificationRow(item: AppNotification, isNew: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TypeBadge(item.type)
            Text(item.type.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = typeColor(item.type), modifier = Modifier.weight(1f))
            if (isNew) Text("새 알림  ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Orange700)
            Text(timeAgo(item.createdAt), fontSize = 14.sp, color = TextSub)
        }
        Text(item.message, fontSize = 17.sp, lineHeight = 24.sp, color = TextStrong)
        Text("${item.type.action} ›", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Orange700)
    }
}

/** 어느 화면에서든 잠깐 뜬다. 쓰던 내용을 잃지 않게 화면을 옮기지 않는다. 사라져도 알림함에 남는다 */
@Composable
fun NotificationBanner(notification: AppNotification, onDismiss: () -> Unit) {
    LaunchedEffect(notification.id) {
        delay(BANNER_MILLIS)
        onDismiss()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .safeDrawingPadding()
            .padding(12.dp)
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .ruled()
            .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        TypeBadge(notification.type)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(notification.type.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = typeColor(notification.type))
            Text(notification.message, fontSize = 16.sp, lineHeight = 22.sp, color = TextStrong)
        }
        IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "닫기", tint = TextSub) }
    }
}
