package com.ieo.mobile.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ieo.mobile.ui.theme.CanvasGray
import com.ieo.mobile.ui.theme.Orange50
import com.ieo.mobile.ui.theme.Orange600
import com.ieo.mobile.ui.theme.Orange700
import com.ieo.mobile.ui.theme.TextMid
import com.ieo.mobile.ui.theme.TextStrong
import com.ieo.mobile.ui.theme.TextSub
import com.ieo.mobile.ui.theme.White
import org.junit.Assert.assertTrue
import org.junit.Test

/** UX.md: 글자 대비 4.5:1, 큰 굵은 글자 3:1 */
class UxContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val x = maxOf(a.luminance(), b.luminance()) + 0.05
        val y = minOf(a.luminance(), b.luminance()) + 0.05
        return (x / y).toDouble()
    }

    @Test
    fun `글자 색은 흰 바탕과 회색 바탕에서 4점5 이상이다`() {
        listOf(TextStrong, TextMid, TextSub).forEach { color ->
            listOf(White, CanvasGray).forEach { background -> assertTrue("$color on $background", ratio(color, background) >= 4.5) }
        }
        assertTrue(ratio(Orange700, White) >= 4.5)
        assertTrue(ratio(Orange700, Orange50) >= 4.5)
    }

    @Test
    fun `주황 버튼의 큰 굵은 글자는 3 이상이다`() {
        assertTrue(ratio(White, Orange600) >= 3.0)
    }
}
