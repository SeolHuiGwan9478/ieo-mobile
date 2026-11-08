package com.ieo.mobile.ui.carlogin

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.ieo.mobile.domain.carlogin.CarLoginCode

/** 카메라 밝기 데이터(Y 평면)에서 차량 QR의 로그인 코드를 읽는다. 없거나 차량 QR이 아니면 null */
class CarLoginQrDecoder {

    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }

    fun decode(luminance: ByteArray, rowStride: Int, width: Int, height: Int): String? {
        val source = PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, width, height, false)
        return try {
            CarLoginCode.fromQr(reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text)
        } catch (e: NotFoundException) {
            null
        } finally {
            reader.reset()
        }
    }
}
