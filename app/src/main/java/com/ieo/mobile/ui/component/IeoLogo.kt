package com.ieo.mobile.ui.component

import com.ieo.mobile.ui.theme.Orange500
import com.ieo.mobile.ui.theme.White
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ieo.mobile.R

/**
 * Ieo 아이콘: 둥근 사각형 안에 누운 i와 작아지는 점 두 개. (design/logo/icon)
 * 기본은 주황 사각형에 흰 심볼 (design/logo/icon/ieo_icon_orange.svg).
 */
@Composable
fun IeoSymbol(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    background: Color = Orange500,
    foreground: Color = White,
) {
    Canvas(modifier = modifier.size(size)) {
        // 원본 SVG의 120 단위 좌표를 그대로 쓰고 실제 크기에 맞춰 늘린다
        val unit = this.size.width / 120f
        drawRoundRect(color = background, cornerRadius = CornerRadius(26 * unit))
        drawRoundRect(
            color = foreground,
            topLeft = Offset(18 * unit, 51 * unit),
            size = Size(42 * unit, 18 * unit),
            cornerRadius = CornerRadius(9 * unit),
        )
        drawCircle(color = foreground, radius = 9 * unit, center = Offset(76 * unit, 60 * unit))
        drawCircle(color = foreground, radius = 6 * unit, center = Offset(98 * unit, 60 * unit))
    }
}

/** 텍스트 로고 "ieo.". 흰·밝은 바탕에는 주황, 주황·어두운 바탕에는 흰색으로 쓴다 */
@Composable
fun IeoWordmark(
    modifier: Modifier = Modifier,
    width: Dp = 240.dp,
    color: Color = Orange500,
) {
    Image(
        painter = painterResource(R.drawable.ieo_wordmark),
        contentDescription = stringResource(R.string.app_name),
        colorFilter = ColorFilter.tint(color),
        modifier = modifier.width(width),
    )
}
