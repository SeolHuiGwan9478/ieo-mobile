package com.ieo.mobile.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ieo.mobile.domain.vehicle.BodyType

private val CabOrange = Color(0xFFFF6C37)
private val CabStripe = Color(0xFFE2501B)
private val Tire = Color(0xFF2B2F36)
private val Hub = Color(0xFFE8EBEF)
private val Chassis = Color(0xFF3A3F47)
private val Glass = Color(0xFFCFE8F6)
private val Headlight = Color(0xFFFFD54A)
private val BoxFill = Color(0xFFF2F4F6)
private val BoxEdge = Color(0xFFC9CED5)
private val Wood = Color(0xFFD9C2A0)
private val WoodLine = Color(0xFFBFA57F)
private val Crate = Color(0xFFC98F55)
private val CrateDark = Color(0xFFB97B42)
private val CrateTape = Color(0xFFE7C79C)
private val Wing = Color(0xFF6FA8DC)
private val ColdFill = Color(0xFFE6F3FC)
private val ColdEdge = Color(0xFFB8D9F0)
private val Snow = Color(0xFF3B9AE1)
private val Unit = Color(0xFF9AA5B1)
private val UnitLine = Color(0xFFDDE2E8)
private val Tarp = Color(0xFF5E9E6E)
private val TarpLine = Color(0xFF4C8A5C)

/**
 * 차종을 옆에서 본 트럭 색 그림 (시안 B). 앞(운전석)이 오른쪽이고, 운전석은 브랜드 주황으로 같다.
 * 짐칸 색과 짐 모양으로 차종을 구분한다. 126×80 좌표(왼쪽 −4부터)로 그리고 실제 크기에 맞춰 늘린다.
 */
@Composable
fun TruckIcon(type: BodyType, modifier: Modifier = Modifier, width: Dp = 76.dp) {
    Canvas(modifier.size(width, width * (80f / 126f))) {
        val g = Grid(size.width / 126f)
        with(g) {
            body(type)
            rect(8f, 60f, 106f, 6f, 3f, Chassis)
            cab()
            listOf(32f, 98f).forEach { x ->
                circle(x, 66f, 9f, Tire)
                circle(x, 66f, 3.6f, Hub)
            }
        }
    }
}

/** viewBox 좌표(x는 −4부터)를 화면 좌표로 바꿔 그린다 */
private class Grid(val u: Float) {
    fun DrawScope.p(x: Float, y: Float) = Offset((x + 4f) * u, y * u)

    fun DrawScope.rect(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Color, edge: Color? = null) {
        drawRoundRect(fill, p(x, y), Size(w * u, h * u), CornerRadius(r * u))
        if (edge != null) drawRoundRect(edge, p(x, y), Size(w * u, h * u), CornerRadius(r * u), style = Stroke(1.5f * u))
    }

    fun DrawScope.circle(x: Float, y: Float, r: Float, fill: Color) = drawCircle(fill, r * u, p(x, y))

    fun DrawScope.line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) =
        drawLine(color, p(x1, y1), p(x2, y2), width * u, StrokeCap.Round)

    fun DrawScope.shape(vararg points: Pair<Float, Float>, fill: Color) {
        val path = Path()
        points.forEachIndexed { i, (x, y) ->
            val o = p(x, y)
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        path.close()
        drawPath(path, fill)
    }

    fun DrawScope.cab() {
        val cab = Path().apply {
            val a = p(80f, 64f); moveTo(a.x, a.y)
            p(80f, 34f).let { lineTo(it.x, it.y) }
            p(80f, 27f).let { c -> p(87f, 27f).let { quadraticTo(c.x, c.y, it.x, it.y) } }
            p(99f, 27f).let { lineTo(it.x, it.y) }
            p(104f, 27f).let { c -> p(107f, 32f).let { quadraticTo(c.x, c.y, it.x, it.y) } }
            p(114f, 44f).let { lineTo(it.x, it.y) }
            p(116f, 47f).let { c -> p(116f, 51f).let { quadraticTo(c.x, c.y, it.x, it.y) } }
            p(116f, 64f).let { lineTo(it.x, it.y) }
            close()
        }
        drawPath(cab, CabOrange)
        shape(88f to 32f, 99f to 32f, 106f to 44f, 88f to 44f, fill = Glass)
        rect(84f, 49f, 10f, 3f, 1.5f, CabStripe)
        rect(113f, 56f, 5f, 8f, 2f, Tire)
        circle(114f, 51f, 2.2f, Headlight)
    }

    fun DrawScope.body(type: BodyType) {
        when (type) {
            BodyType.CARGO -> {
                rect(8f, 50f, 68f, 10f, 2f, Wood)
                line(8f, 53f, 76f, 53f, WoodLine, 1.5f)
                rect(14f, 34f, 20f, 16f, 2f, Crate)
                rect(36f, 38f, 16f, 12f, 2f, CrateDark)
                rect(54f, 30f, 18f, 20f, 2f, Crate)
                line(24f, 34f, 24f, 50f, CrateTape, 2f)
                line(44f, 38f, 44f, 50f, CrateTape, 2f)
                line(63f, 30f, 63f, 50f, CrateTape, 2f)
            }
            BodyType.WING -> {
                rect(8f, 30f, 68f, 30f, 3f, BoxFill, BoxEdge)
                rect(12f, 34f, 12f, 9f, 1.5f, Crate)
                rect(26f, 37f, 10f, 6f, 1.5f, CrateDark)
                // 지붕 가운데를 축으로 양쪽 옆판이 날개처럼 들린다
                shape(42f to 30f, 10f to 12f, 14f to 8f, 42f to 26f, 70f to 8f, 74f to 12f, fill = Wing)
            }
            BodyType.BOX -> {
                rect(8f, 22f, 68f, 38f, 3f, BoxFill, BoxEdge)
                line(16f, 26f, 16f, 56f, BoxEdge, 1.5f)
                line(24f, 26f, 24f, 56f, BoxEdge, 1.5f)
                rect(34f, 34f, 34f, 6f, 3f, CabOrange)
            }
            BodyType.REFRIGERATED -> {
                rect(8f, 22f, 68f, 38f, 3f, ColdFill, ColdEdge)
                rect(64f, 14f, 12f, 9f, 2f, Unit)
                line(67f, 17f, 73f, 17f, UnitLine, 1.3f)
                line(67f, 20f, 73f, 20f, UnitLine, 1.3f)
                line(40f, 31f, 40f, 51f, Snow, 2.4f)
                line(31.3f, 36f, 48.7f, 46f, Snow, 2.4f)
                line(31.3f, 46f, 48.7f, 36f, Snow, 2.4f)
            }
            BodyType.LIFT -> {
                rect(14f, 22f, 62f, 38f, 3f, BoxFill, BoxEdge)
                rect(40f, 34f, 28f, 6f, 3f, CabOrange)
                line(14f, 56f, 6f, 66f, Chassis, 2.4f)
                rect(-1f, 66f, 18f, 4f, 2f, Chassis)
                rect(2f, 56f, 9f, 10f, 1.5f, Crate)
            }
            BodyType.ETC -> {
                rect(8f, 50f, 68f, 10f, 2f, Wood)
                // 덮개를 씌운 짐
                val tarp = Path().apply {
                    p(12f, 50f).let { moveTo(it.x, it.y) }
                    p(16f, 30f).let { c -> p(42f, 28f).let { quadraticTo(c.x, c.y, it.x, it.y) } }
                    p(68f, 30f).let { c -> p(72f, 50f).let { quadraticTo(c.x, c.y, it.x, it.y) } }
                    close()
                }
                drawPath(tarp, Tarp)
                val fold = Path().apply {
                    p(24f, 50f).let { moveTo(it.x, it.y) }
                    p(28f, 38f).let { c -> p(42f, 37f).let { quadraticTo(c.x, c.y, it.x, it.y) } }
                    p(56f, 38f).let { c -> p(60f, 50f).let { quadraticTo(c.x, c.y, it.x, it.y) } }
                }
                drawPath(fold, TarpLine, style = Stroke(2f * u))
            }
        }
    }
}
