package com.ieo.mobile.ui.ledger

import com.ieo.mobile.ui.theme.White
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.domain.ledger.LedgerEntry
import com.ieo.mobile.ui.component.QuietButton
import com.ieo.mobile.ui.theme.IeoTag
import com.ieo.mobile.ui.theme.TagColor
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import java.time.YearMonth

/** 그래프에 쓰는 짧은 금액: "412만" */
fun shortWon(amount: Long): String = "${Math.round(amount / 10_000.0)}만"

fun monthLabel(month: YearMonth, thisYear: Int): String =
    if (month.year == thisYear) "${month.monthValue}월" else "${month.year}년 ${month.monthValue}월"

/** 돈을 받았는지. 색과 글자를 함께 쓴다 */
fun paidTag(entry: LedgerEntry): Pair<String, TagColor> = when {
    entry.paid -> "받음" to IeoTag.Sage
    entry.overdue -> "예정일 지남" to IeoTag.Plum
    else -> "못 받음" to IeoTag.Sand
}

@Composable
fun StatusTag(text: String, color: TagColor) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color.content)
}

/** 저장 뒤 알림과 되돌리기 */
@Composable
fun NoticeBar(notice: LedgerNotice, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(TextStrong, RoundedCornerShape(14.dp)).clickable(onClick = onDismiss).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(notice.text, fontSize = 16.sp, color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
        notice.undo?.let { undo ->
            androidx.compose.material3.TextButton(onClick = undo) { Text("되돌리기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFFFC9B3)) }
        }
    }
}

@Composable
fun AmountLine(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 16.sp, color = TextMid)
        Text(value, fontSize = 16.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.SemiBold, color = TextStrong)
    }
}
