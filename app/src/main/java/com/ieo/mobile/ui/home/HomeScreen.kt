package com.ieo.mobile.ui.home

import com.ieo.mobile.ui.component.ruled
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import com.ieo.mobile.ui.component.ActionRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.domain.document.DriverDocuments
import com.ieo.mobile.ui.document.VerifiedBadge
import com.ieo.mobile.domain.trip.Trip
import com.ieo.mobile.ui.common.DayTimeFormatter
import com.ieo.mobile.ui.common.formatWon
import com.ieo.mobile.ui.component.IeoSymbol
import com.ieo.mobile.ui.component.PrimaryButton
import com.ieo.mobile.ui.notification.NotificationBell
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.White
import com.ieo.mobile.ui.trip.TripCard
import com.ieo.mobile.ui.vehicle.VehicleSummaryRow
import com.ieo.mobile.ui.vehicle.VehiclesState

/** 홈 (M07). 수익은 장부를 연결하면 더한다 */
@Composable
fun HomeScreen(
    name: String?,
    vehicles: VehiclesState,
    nextTrip: Trip?,
    monthNet: Long?,
    owed: Long,
    invitationCount: Int,
    onInvitations: () -> Unit,
    onEarnings: () -> Unit,
    onScan: () -> Unit,
    onVehicles: () -> Unit,
    onOpenTrip: (Trip) -> Unit,
    onFindOrders: () -> Unit,
    unreadNotifications: Int = 0,
    onNotifications: () -> Unit = {},
    documents: DriverDocuments? = null,
    onDocuments: () -> Unit = {},
) {
    val labels = remember { DayTimeFormatter() }
    Column(
        Modifier.fillMaxSize().background(White).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IeoSymbol(size = 32.dp)
            Text(name?.takeIf { it.isNotBlank() }?.let { "${it}님" } ?: "이에오", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            if (documents?.verified == true) VerifiedBadge()
            Spacer(Modifier.weight(1f))
            NotificationBell(unreadNotifications, onNotifications)
        }
        documents?.takeIf { !it.verified }?.let { DocumentsCard(it, onDocuments) }
        if (invitationCount > 0) {
            Column(Modifier.fillMaxWidth().ruled().clickable(onClick = onInvitations).padding(vertical = 18.dp)) {
                Text("단골 요청 ${invitationCount}건이 왔어요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Orange700)
                Text("눌러서 답해 주세요", fontSize = 16.sp, color = TextMid)
            }
        }
        if (monthNet != null) {
            Column(
                Modifier.fillMaxWidth().ruled().clickable(onClick = onEarnings).padding(vertical = 20.dp),
            ) {
                Text("이번 달 순수익", fontSize = 16.sp, color = TextMid)
                Text(formatWon(monthNet), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                if (owed > 0) Text("못 받은 돈 ${formatWon(owed)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = IeoStatus.colors.warning)
            }
        }
        if (nextTrip != null) {
            TripCard(nextTrip, labels) { onOpenTrip(nextTrip) }
        } else {
            Section {
                Text("오늘 잡힌 배차가 없어요", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextStrong)
                Spacer(Modifier.height(10.dp))
                PrimaryButton("오더 찾기", onFindOrders)
            }
        }
        ActionRow(Icons.Outlined.QrCodeScanner, "차량에 로그인", "차량 화면의 QR 찍기", onScan)
        Section { VehicleSummaryRow(vehicles, onVehicles) }
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
        Modifier.fillMaxWidth().ruled().clickable(onClick = onClick).padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Orange700)
            Text(detail, fontSize = 16.sp, color = TextMid)
        }
        Text("${documents.approvedRequired}/${documents.requiredTotal}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        Text("  ›", fontSize = 20.sp, color = TextMid)
    }
}

@Composable
private fun Section(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().ruled().padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}
