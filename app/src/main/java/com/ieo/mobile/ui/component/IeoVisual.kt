package com.ieo.mobile.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import com.ieo.mobile.ui.component.ruled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

/** 뜻을 먼저 알려 주는 동그란 아이콘. 글자를 대신하지 않고 곁에 둔다 */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = TextStrong, @Suppress("UNUSED_PARAMETER") background: Color = Color.Transparent, size: Dp = 44.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.CenterStart) {
        Icon(icon, contentDescription = null, tint = if (tint == Orange700) TextStrong else tint, modifier = Modifier.size(26.dp))
    }
}

/** 눌러서 들어가는 한 줄: 아이콘 + 제목 + 작은 설명 + 화살표 */
@Composable
fun ActionRow(icon: ImageVector, title: String, detail: String?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().ruled().clickable(onClick = onClick).padding(vertical = 16.dp).semantics(mergeDescendants = true) {},
    ) {
        IconBadge(icon)
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextStrong)
            detail?.let { Text(it, fontSize = 15.sp, color = TextMid) }
        }
        Text("›", fontSize = 24.sp, color = TextSub)
    }
}

/** 아이콘 위, 항목 이름, 값이 쌓인 작은 칸. 한눈에 비교하는 정보에 쓴다 */
@Composable
fun InfoTile(@DrawableRes icon: Int, label: String, value: String, modifier: Modifier = Modifier, known: Boolean = true) {
    Column(modifier.ruled().padding(vertical = 16.dp).semantics(mergeDescendants = true) {}) {
        // 설정 메뉴처럼 색을 채운 아이콘 (현장 정보 카드)
        Icon(painterResource(icon), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(32.dp))
        Spacer(Modifier.size(10.dp))
        Text(label, fontSize = 15.sp, color = TextSub)
        Text(value, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = if (known) FontWeight.Bold else FontWeight.Medium, color = if (known) TextStrong else TextSub)
    }
}

/** 순서대로 진행되는 칸을 점과 선으로 보여 준다. 시각이 있는 칸까지 채운다 */
@Composable
fun StepTimeline(steps: List<Pair<String, String?>>) {
    Row(Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, (label, value) ->
            val done = value != null
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).size(height = 2.dp, width = 1.dp).background(if (index > 0 && done) Orange700 else if (index > 0) LINE else Color.Transparent))
                    Box(Modifier.size(14.dp).background(if (done) Orange700 else LINE, CircleShape))
                    val nextDone = steps.getOrNull(index + 1)?.second != null
                    Box(Modifier.weight(1f).size(height = 2.dp, width = 1.dp).background(if (index < steps.lastIndex && nextDone) Orange700 else if (index < steps.lastIndex) LINE else Color.Transparent))
                }
                Spacer(Modifier.width(1.dp).size(6.dp))
                Text(label, fontSize = 15.sp, color = TextSub)
                Text(value ?: "—", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (done) TextStrong else TextSub)
            }
        }
    }
}

private val LINE = Color(0xFFD1D6DB)
