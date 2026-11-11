package com.ieo.mobile.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.domain.trip.CancelReason
import com.ieo.mobile.domain.trip.Trip
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong

/**
 * 운행 취소 확인 (DSP-030, 031, 033). 직접 등록한 운행은 묻기만 하고,
 * 이에오 배차는 사유를 골라야 하며, 상차 예정 2시간 이내면 늦은 취소 경고를 보여 준다
 */
@Composable
fun CancelTripDialog(trip: Trip, late: Boolean, onCancel: (CancelReason?) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf<CancelReason?>(null) }
    val ready = !trip.needsCancelReason || reason != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (trip.needsCancelReason) "배차를 취소할까요?" else "운행을 취소할까요?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (late) {
                    Text(
                        "상차 2시간 이내라 '늦은 취소'로 주선사에게 보이는 내 기록에 남아요. 30일에 3번이면 7일 동안 배차를 요청할 수 없어요.",
                        color = IeoStatus.colors.warning,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                }
                if (trip.needsCancelReason) {
                    Text("취소 사유", color = TextMid, fontSize = 15.sp)
                    CancelReason.entries.forEach { option ->
                        val selected = option == reason
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Orange50 else Color.Transparent)
                                .clickable { reason = option }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(
                                option.label,
                                fontSize = 16.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Orange700 else TextStrong,
                            )
                        }
                    }
                } else {
                    Text("취소하면 되돌릴 수 없어요.", color = TextMid, fontSize = 16.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCancel(reason) }, enabled = ready) {
                Text("취소하기", color = if (ready) IeoStatus.colors.warning else TextMid, fontSize = 16.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("아니요", color = TextMid, fontSize = 16.sp) } },
    )
}
