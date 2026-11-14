package com.ieo.mobile.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ieo.mobile.ui.theme.Orange500

private const val CYCLE_MILLIS = 1800

/** 로고 심볼 좌표(120 단위)에서 막대와 점 두 개만 잘라낸 영역 */
private const val GLYPH_LEFT = 14f
private const val GLYPH_WIDTH = 90f
private const val GLYPH_HEIGHT = 26f

/**
 * 로딩 표시 "이어 붙이기" (2026-11-14). 로고의 막대가 앞으로 늘어나 큰 점·작은 점을 차례로 이어 붙였다가 돌아온다.
 * 차량 앱(IeoLoading.kt)·주선사 콘솔(IeoLoader)과 같은 움직임이다. 동그라미 로딩 대신 쓴다
 */
@Composable
fun IeoLoadingIndicator(
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
    color: Color = Orange500,
) {
    val phase by rememberInfiniteTransition(label = "ieo-loading").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(CYCLE_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(
        modifier = modifier
            .size(width = width, height = width * (GLYPH_HEIGHT / GLYPH_WIDTH))
            .semantics { contentDescription = "불러오는 중" },
    ) {
        val unit = size.width / GLYPH_WIDTH
        fun x(value: Float) = (value - GLYPH_LEFT) * unit
        val centerY = size.height / 2
        val frame = IeoLoadingMotion.at(phase)
        val barHeight = 18 * unit
        drawRoundRect(
            color = color,
            topLeft = Offset(x(18f), centerY - barHeight / 2),
            size = Size(frame.barWidth * unit, barHeight),
            cornerRadius = CornerRadius(barHeight / 2),
        )
        drawCircle(color = color.copy(alpha = color.alpha * frame.dotAlpha), radius = 9 * unit, center = Offset(x(76f), centerY))
        drawCircle(color = color.copy(alpha = color.alpha * frame.smallDotAlpha), radius = 6 * unit, center = Offset(x(98f), centerY))
    }
}

/**
 * "이어 붙이기" 한 주기(0~1)의 모양. 로고 좌표 단위.
 * 막대 42 → 86(작은 점 끝까지)으로 늘었다가 돌아오고, 막대가 지나가는 동안 점이 사라졌다가 돌아온다
 */
internal object IeoLoadingMotion {
    data class Frame(val barWidth: Float, val dotAlpha: Float, val smallDotAlpha: Float)

    private const val SHORT = 42f
    private const val LONG = 86f
    private val ease = FastOutSlowInEasing

    fun at(phase: Float): Frame {
        val p = ((phase % 1f) + 1f) % 1f
        val bar = when {
            p < 0.10f -> SHORT
            p < 0.50f -> SHORT + (LONG - SHORT) * ease.transform((p - 0.10f) / 0.40f)
            p < 0.60f -> LONG
            p < 0.95f -> LONG - (LONG - SHORT) * ease.transform((p - 0.60f) / 0.35f)
            else -> SHORT
        }
        return Frame(bar, fade(p, 0.20f, 0.30f, 0.72f, 0.80f), fade(p, 0.38f, 0.46f, 0.60f, 0.70f))
    }

    /** outStart~outEnd 동안 1→0, inStart~inEnd 동안 0→1 */
    private fun fade(p: Float, outStart: Float, outEnd: Float, inStart: Float, inEnd: Float): Float = when {
        p < outStart -> 1f
        p < outEnd -> 1f - (p - outStart) / (outEnd - outStart)
        p < inStart -> 0f
        p < inEnd -> (p - inStart) / (inEnd - inStart)
        else -> 1f
    }
}
