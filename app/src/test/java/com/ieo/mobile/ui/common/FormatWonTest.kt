package com.ieo.mobile.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatWonTest {

    @Test
    fun `만 원 단위는 만 원으로, 나머지는 쉼표로, 마이너스도 같은 모양으로`() {
        assertEquals("45만 원", formatWon(450_000))
        assertEquals("9,000원", formatWon(9_000))
        assertEquals("−102만 원", formatWon(-1_020_000))
        assertEquals("−9,500원", formatWon(-9_500))
    }
}
