package com.ieo.mobile.ui

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.ieo.mobile.ui.carlogin.CarLoginQrDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarLoginQrDecoderTest {

    private val decoder = CarLoginQrDecoder()

    /** 카메라처럼 줄 끝 여백이 있는 밝기 데이터로 QR을 그린다 */
    private fun frame(content: String, size: Int = 480, stride: Int = 512): ByteArray {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bytes = ByteArray(stride * size) { 0xFF.toByte() }
        for (y in 0 until size) for (x in 0 until size) if (matrix[x, y]) bytes[y * stride + x] = 0
        return bytes
    }

    @Test
    fun `차량 QR 이미지에서 코드를 읽는다`() {
        assertEquals("KX7P-M4QZ", decoder.decode(frame("ieo://car-login?code=KX7P-M4QZ"), 512, 480, 480))
    }

    @Test
    fun `다른 QR이나 빈 화면은 무시한다`() {
        assertNull(decoder.decode(frame("https://example.com"), 512, 480, 480))
        assertNull(decoder.decode(ByteArray(512 * 480) { 0x7F }, 512, 480, 480))
    }

    @Test
    fun `읽은 뒤에도 다음 프레임을 읽는다`() {
        decoder.decode(ByteArray(512 * 480), 512, 480, 480)
        assertEquals("AB12-CD34", decoder.decode(frame("ieo://car-login?code=AB12-CD34"), 512, 480, 480))
    }
}
