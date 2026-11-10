package com.ieo.mobile.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.domain.vehicle.BodyType
import com.ieo.mobile.domain.vehicle.Tonnage
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.White

/*
 * 차량 고르기 부품: 가입(4/5 톤수, 5/5 차종)과 내 차량 수정에서 같이 쓴다
 */

/** 큰 톤수 표시 양옆의 −/+. 끝에서는 그 쪽 버튼이 꺼진다 */
@Composable
fun TonnageStepper(tonnage: Tonnage, onStep: (Int) -> Unit) {
    val first = tonnage == Tonnage.entries.first()
    val last = tonnage == Tonnage.entries.last()
    Row(
        Modifier.fillMaxWidth().border(1.dp, LineGray, RoundedCornerShape(12.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−", "톤수 내리기", enabled = !first) { onStep(-1) }
        Text(tonnage.label, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextStrong, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        StepperButton("+", "톤수 올리기", enabled = !last) { onStep(1) }
    }
}

@Composable
private fun StepperButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .background(if (enabled) CanvasGray else White, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Text(symbol, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = if (enabled) TextStrong else LineGray)
    }
}

/** 톤수 바로 고르기: 한 줄로 밀어서 본다 */
@Composable
fun TonnageChips(selected: Tonnage, onSelect: (Tonnage) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Tonnage.entries.forEach { BrandChip(it.label, selected == it) { onSelect(it) } }
    }
}

/** 차종: 트럭 그림이 있는 네모 칸 2열 */
@Composable
fun BodyTypeGrid(selected: BodyType, onSelect: (BodyType) -> Unit) {
    Column {
        BodyType.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                pair.forEach { type -> BodyTile(type, selected == type, Modifier.weight(1f)) { onSelect(type) } }
            }
        }
    }
}

@Composable
private fun BodyTile(type: BodyType, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .aspectRatio(1.15f)
            .background(if (selected) Orange50 else White, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) Orange600 else LineGray, shape)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected }
            .padding(12.dp),
    ) {
        Spacer(Modifier.weight(1f))
        TruckIcon(type, width = 84.dp)
        Text(type.label, fontSize = 17.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) Orange700 else TextStrong)
        Spacer(Modifier.weight(1f))
    }
}

