package com.ieo.mobile.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ieo.mobile.domain.plan.PlanCode
import com.ieo.mobile.ui.theme.Orange100
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White

/** 티켓 오른쪽 떼는 부분의 너비. 시안(docs/design/assets/plan-tickets)의 360 중 98과 같은 비율 */
private val STUB_WIDTH = 96.dp
private val NOTCH = 11.dp
private val CORNER = 16.dp

/**
 * 플랜 티켓 모양 (PLAN-007): 둥근 사각형에서 떼는 선 자리 위아래를 반원으로 파낸다.
 * stubWidth는 오른쪽 끝에서 떼는 선까지 거리
 */
class TicketShape(private val stubWidth: Dp = STUB_WIDTH, private val notch: Dp = NOTCH, private val corner: Dp = CORNER) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { notch.toPx() }
        val c = with(density) { corner.toPx() }
        val x = size.width - with(density) { stubWidth.toPx() }
        val body = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(c))) }
        val holes = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(Offset(x, 0f), r))
            addOval(androidx.compose.ui.geometry.Rect(Offset(x, size.height), r))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, body, holes))
    }
}

/** 플랜별 티켓 색. 베이직 흰색, 프로 주황 (검정 프로 플러스 시안은 docs/design/assets에 남겨 둠) */
private data class TicketColors(val fill: Color, val border: Color?, val ink: Color, val sub: Color, val title: Color, val dot: Color, val perforation: Color)

private fun colorsOf(code: PlanCode) = when (code) {
    PlanCode.BASIC -> TicketColors(White, Color(0xFFE5E8EB), TextStrong, TextSub, TextStrong, Orange600, Color(0xFFD1D6DB))
    PlanCode.PRO -> TicketColors(Orange600, null, White, Orange100, White, White, White)
}

/**
 * 플랜 티켓 한 장. 왼쪽에 이름과 세 줄, 오른쪽 떼는 부분에 [stubTitle](가격이나 D-23)과 [stubSub].
 * [badge]가 있으면 이름 옆에 작은 꼬리표("쓰는 중")를 단다
 */
@Composable
fun PlanTicket(
    code: PlanCode,
    label: String,
    highlights: List<String>,
    stubTitle: String,
    stubSub: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val colors = colorsOf(code)
    val shape = TicketShape()
    var box = modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 150.dp).background(colors.fill, shape)
    if (colors.border != null) box = box.border(1.5.dp, colors.border, shape)
    Row(
        box.drawBehind {
            val x = size.width - STUB_WIDTH.toPx()
            val inset = NOTCH.toPx() + 6.dp.toPx()
            drawLine(
                colors.perforation.copy(alpha = 0.7f), Offset(x, inset), Offset(x, size.height - inset),
                strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx())),
            )
        },
    ) {
        Column(Modifier.weight(1f).fillMaxHeight().padding(start = 22.dp, end = 12.dp, top = 16.dp, bottom = 18.dp)) {
            Text("이에오 이용권", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.sub)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = colors.title)
                if (badge != null) {
                    Text(
                        badge, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.fill,
                        modifier = Modifier.padding(start = 8.dp).background(colors.ink, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Column(Modifier.padding(top = 6.dp)) {
                highlights.take(3).forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                        Box(Modifier.size(5.dp).background(colors.dot, CircleShape))
                        Text(line, fontSize = 14.sp, color = colors.ink, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        Column(Modifier.width(STUB_WIDTH).fillMaxHeight(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stubTitle, fontSize = if (stubTitle.length > 6) 17.sp else 20.sp, fontWeight = FontWeight.Bold, color = colors.ink, textAlign = TextAlign.Center)
            Text(stubSub, fontSize = 13.sp, color = colors.sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** 9900 → "9,900원", 0 → "무료" */
fun priceLabel(price: Int): String = if (price == 0) "무료" else "%,d원".format(price)
