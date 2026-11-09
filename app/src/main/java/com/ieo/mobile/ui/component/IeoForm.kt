package com.ieo.mobile.ui.component

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
import com.ieo.mobile.ui.theme.White

private val ButtonShape = RoundedCornerShape(16.dp)
private val ChipOnLine = Color(0xFFFFC9B3)
private val ChipOffLine = Color(0xFFD1D6DB)

/** 화면의 주요 행동. [busy]면 진행 표시를 돌리고 누르지 못한다 */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier, busy: Boolean = false, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = Orange600, contentColor = White),
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(color = TextMid, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
        } else {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(24.dp)) }
            Text(text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TonalButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = CanvasGray, contentColor = TextMid),
        modifier = modifier.heightIn(min = 60.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(22.dp)) }
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 16.sp) },
        singleLine = true,
        shape = ButtonShape,
        textStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = TextStrong),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

/** 단계 표시: "1 / 4 · 휴대폰 인증" */
@Composable
fun StepLabel(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Orange700, modifier = Modifier.padding(bottom = 6.dp))
}

/** 시안의 큰 제목 */
@Composable
fun Heading(text: String) {
    Text(text, fontSize = 27.sp, lineHeight = 37.sp, fontWeight = FontWeight.ExtraBold, color = TextStrong)
}

/** 고르기 칩. 장갑을 껴도 누르도록 높이 48dp */
@Composable
fun BrandChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .background(if (selected) Orange50 else White, shape)
            .border(if (selected) 2.dp else 1.5.dp, if (selected) ChipOnLine else ChipOffLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) Orange700 else TextMid)
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
            Column(Modifier.fillMaxWidth().background(White, RoundedCornerShape(20.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Modifier.fillMaxWidth().background(Color(0xFFFDECEC), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, fontSize = 16.sp, lineHeight = 23.sp, color = IeoStatus.colors.warning, modifier = Modifier.weight(1f).padding(vertical = 6.dp))
        if (onRetry != null) QuietButton("다시 시도", onRetry)
    }
}
