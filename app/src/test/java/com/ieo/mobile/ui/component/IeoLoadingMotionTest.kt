package com.ieo.mobile.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

/** 로딩 "이어 붙이기": 막대가 늘어나 점을 이어 붙였다가 돌아온다 */
class IeoLoadingMotionTest {

    @Test
    fun `처음과 끝은 로고 그대로이고 가운데서 막대가 작은 점 끝까지 늘어나 점이 사라진다`() {
        val start = IeoLoadingMotion.at(0f)
        assertEquals(42f, start.barWidth, 0.01f)
        assertEquals(1f, start.dotAlpha, 0.01f)
        assertEquals(1f, start.smallDotAlpha, 0.01f)

        val joined = IeoLoadingMotion.at(0.55f)
        assertEquals(86f, joined.barWidth, 0.01f)
        assertEquals(0f, joined.dotAlpha, 0.01f)
        assertEquals(0f, joined.smallDotAlpha, 0.01f)

        val end = IeoLoadingMotion.at(0.99f)
        assertEquals(42f, end.barWidth, 0.01f)
        assertEquals(1f, end.dotAlpha, 0.01f)
        assertEquals(IeoLoadingMotion.at(1.0f), IeoLoadingMotion.at(0f))
    }
}
