package com.ieo.mobile.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.IeoStatus
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

private val ButtonShape = RoundedCornerShape(10.dp)
private val ChipShape = RoundedCornerShape(8.dp)

/** 묶음을 카드 대신 얇은 선으로 나눈다 */
val LineGray = Color(0xFFE5E8EB)

fun Modifier.ruled(): Modifier = drawBehind {
    drawLine(LineGray, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
}

/** 화면의 주요 행동. [busy]면 진행 표시를 돌리고 누르지 못한다 */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier, busy: Boolean = false, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = White),
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(color = TextMid, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
        } else {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(22.dp)) }
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun TonalButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = TextStrong),
        border = BorderStroke(1.dp, LineGray),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(22.dp)) }
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMid)
    }
}

@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    /** 칸 안쪽 오른쪽에 두는 버튼이나 표시 (예: 인증 요청) */
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 16.sp) },
        placeholder = placeholder?.let { { Text(it, fontSize = 19.sp, color = TextSub) } },
        singleLine = true,
        shape = ButtonShape,
        textStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = TextStrong),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

/** 입력칸 안쪽 오른쪽의 작은 글자 버튼 (인증 요청, 확인) */
@Composable
fun FieldAction(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    androidx.compose.material3.TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.padding(end = 4.dp)) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (enabled) Orange700 else TextSub)
    }
}

/** 단계 표시: "1 / 4 · 휴대폰 인증" */
@Composable
fun StepLabel(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Orange700, modifier = Modifier.padding(bottom = 6.dp))
}

/** 시안의 큰 제목 */
@Composable
fun Heading(text: String) {
    Text(text, fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = TextStrong)
}

/** 고르기 칩. 장갑을 껴도 누르도록 높이 48dp */
@Composable
fun BrandChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .background(if (selected) TextStrong else CanvasGray, ChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) White else TextMid)
    }
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, fontSize = 27.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold, color = TextStrong)
}

/** 불러오는 동안 카드 자리를 미리 보여 준다 */
@Composable
fun SkeletonCards(count: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) {
            Column(Modifier.fillMaxWidth().ruled().padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth(0.7f).height(22.dp).background(CanvasGray, RoundedCornerShape(6.dp)))
                Box(Modifier.fillMaxWidth(0.45f).height(18.dp).background(CanvasGray, RoundedCornerShape(6.dp)))
                Box(Modifier.fillMaxWidth(0.9f).height(18.dp).background(CanvasGray, RoundedCornerShape(6.dp)))
            }
        }
    }
}

/** 빈 화면: 왜 비었는지와 다음 행동 */
@Composable
fun EmptyState(title: String, detail: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        Spacer(Modifier.height(6.dp))
        Text(detail, fontSize = 16.sp, lineHeight = 23.sp, color = TextMid)
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            PrimaryButton(action, onAction)
        }
    }
}

/** 실패 안내. [onRetry]가 있으면 다시 시도 버튼을 둔다 */
@Composable
fun ErrorNotice(message: String, onRetry: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().ruled().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, fontSize = 16.sp, lineHeight = 23.sp, color = IeoStatus.colors.warning, modifier = Modifier.weight(1f).padding(vertical = 6.dp))
        if (onRetry != null) QuietButton("다시 시도", onRetry)
    }
}
