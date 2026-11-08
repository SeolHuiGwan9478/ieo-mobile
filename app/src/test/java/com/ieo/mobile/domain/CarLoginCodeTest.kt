package com.ieo.mobile.domain

import com.ieo.mobile.domain.carlogin.CarLoginCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarLoginCodeTest {

    @Test
    fun `차량 QR에서 코드를 꺼낸다`() {
        assertEquals("KX7P-M4QZ", CarLoginCode.fromQr("ieo://car-login?code=KX7P-M4QZ"))
    }

    @Test
    fun `차량 QR이 아니면 무시한다`() {
        assertNull(CarLoginCode.fromQr("https://example.com/?code=KX7P-M4QZ"))
        assertNull(CarLoginCode.fromQr("ieo://car-login?code=KX7"))
    }

    @Test
    fun `직접 입력하면 대문자와 하이픈으로 맞춘다`() {
        assertEquals("KX7P", CarLoginCode.normalize("kx7p"))
        assertEquals("KX7P-M4QZ", CarLoginCode.normalize("kx7p m4qz99"))
        assertTrue(CarLoginCode.isComplete("KX7P-M4QZ"))
        assertFalse(CarLoginCode.isComplete("KX7P-M4"))
    }
}
