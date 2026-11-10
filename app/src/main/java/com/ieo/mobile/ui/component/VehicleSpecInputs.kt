package com.ieo.mobile.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.domain.vehicle.DimensionRange
import com.ieo.mobile.domain.vehicle.VehicleDimensions
import com.ieo.mobile.domain.vehicle.VehicleHeight
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub

private const val HEIGHT_STEP = 0.1

/**
 * 차량 높이와 너비·길이·총중량·축 수 (모두 선택). 테두리 하나 안의 목록으로 보여 주고,
 * 줄마다 동그란 −/+로 맞춘다. 처음 누르면 흔한 값에서 시작하고, 값이 있을 때만 ×로 비운다.
 * 가입의 내 차 화면과 내 차량 수정에서 같이 쓴다.
 */
@Composable
fun VehicleSpecInputs(
    heightMeters: Double?,
    dimensions: VehicleDimensions,
    onHeight: (Double?) -> Unit,
    onDimensions: (VehicleDimensions) -> Unit,
) {
    val d = dimensions
    Column(Modifier.fillMaxWidth().border(1.dp, LineGray, RoundedCornerShape(12.dp))) {
        SpecRow("높이", heightMeters?.let { "%.1fm".format(it) },
            onMinus = { onHeight(VehicleHeight.step(heightMeters, -HEIGHT_STEP)) },
            onPlus = { onHeight(VehicleHeight.step(heightMeters, HEIGHT_STEP)) },
            onClear = { onHeight(null) })
        Divider()
        RangeRow("너비", d.widthCm, VehicleDimensions.WIDTH, ::cmAsMeters) { onDimensions(d.copy(widthCm = it)) }
        Divider()
        RangeRow("길이", d.lengthCm, VehicleDimensions.LENGTH, ::cmAsMeters) { onDimensions(d.copy(lengthCm = it)) }
        Divider()
        RangeRow("총중량", d.totalWeightKg, VehicleDimensions.TOTAL_WEIGHT, ::kgAsTons) { onDimensions(d.copy(totalWeightKg = it)) }
        Divider()
        RangeRow("축 수", d.axleCount, VehicleDimensions.AXLES, { "${it}축" }) { onDimensions(d.copy(axleCount = it)) }
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = LineGray, modifier = Modifier.padding(horizontal = 14.dp))

private fun cmAsMeters(cm: Int): String = "%.1fm".format(cm / 100.0).replace(".0m", "m")

private fun kgAsTons(kg: Int): String = "${kg / 1_000}톤"

@Composable
private fun RangeRow(label: String, value: Int?, range: DimensionRange, format: (Int) -> String, onChange: (Int?) -> Unit) =
    SpecRow(label, value?.let(format),
        onMinus = { onChange(range.next(value, -range.step)) },
        onPlus = { onChange(range.next(value, range.step)) },
        onClear = { onChange(null) })

@Composable
private fun SpecRow(label: String, value: String?, onMinus: () -> Unit, onPlus: () -> Unit, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextStrong, modifier = Modifier.weight(1f))
        RoundButton("−", "$label 줄이기", onMinus)
        Text(
            value ?: "모름",
            fontSize = 17.sp,
            fontWeight = if (value == null) FontWeight.Normal else FontWeight.Bold,
            color = if (value == null) TextSub else TextStrong,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(60.dp),
        )
        RoundButton("+", "$label 늘리기", onPlus)
        // ×는 늘 보이고, 값이 있을 때만 눌린다 (모름이면 흐리게)
        Box(
            Modifier.size(40.dp).clickable(enabled = value != null, role = Role.Button, onClickLabel = "$label 비우기", onClick = onClear),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Close, contentDescription = "$label 비우기", tint = if (value != null) TextSub else LineGray, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
private fun RoundButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(34.dp).background(CanvasGray, CircleShape), contentAlignment = Alignment.Center) {
            Text(symbol, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = TextMid)
        }
    }
}

/** 제원 목록 아래 한 줄. 이름을 자동차등록증 제원 칸(길이·너비·높이·총중량)에 맞췄다 */
@Composable
fun SpecNote() {
    Text(
        "자동차등록증에 적힌 값을 넣어 주세요. 비워 두면 톤수 기준으로 계산해요.",
        fontSize = 14.sp, lineHeight = 20.sp, color = TextSub, modifier = Modifier.padding(top = 8.dp),
    )
}
