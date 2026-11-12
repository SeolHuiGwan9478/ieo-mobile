package com.ieo.mobile.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale

/** 두 번 누르면 이만큼 확대. 다시 두 번 누르면 원래대로 */
private const val DOUBLE_TAP_SCALE = 2.5f
private const val MAX_SCALE = 5f

/**
 * 확대해서 보는 사진. 두 손가락으로 벌리거나(최대 5배) 두 번 눌러 확대하고, 확대한 채로 끌어 옮긴다.
 * 두 번 누른 곳이 손가락 아래에 그대로 남게 확대한다. 사진 밖으로는 끌려 나가지 않는다
 */
@Composable
fun ZoomableImage(bitmap: ImageBitmap, contentDescription: String, modifier: Modifier = Modifier) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(modifier.clipToBounds()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        fun clamp(value: Offset, s: Float): Offset {
            val maxX = width * (s - 1) / 2
            val maxY = height * (s - 1) / 2
            return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
        }
        Image(
            bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap) {
                    detectTapGestures(onDoubleTap = { tap ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = DOUBLE_TAP_SCALE
                            offset = clamp((Offset(width / 2, height / 2) - tap) * (DOUBLE_TAP_SCALE - 1), DOUBLE_TAP_SCALE)
                        }
                    })
                }
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val next = (scale * zoom).coerceIn(1f, MAX_SCALE)
                        scale = next
                        offset = if (next == 1f) Offset.Zero else clamp(offset + pan, next)
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
