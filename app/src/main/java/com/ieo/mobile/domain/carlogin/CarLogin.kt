package com.ieo.mobile.domain.carlogin

import com.ieo.mobile.core.network.ApiResult
import java.time.Instant

/** 승인 화면에 보여 줄 차량 로그인 요청 */
data class CarLoginRequest(
    val code: String,
    val deviceName: String,
    val approxLocation: String?,
    val expiresAt: Instant,
    /** 아직 승인을 기다리는 요청 */
    val isPending: Boolean,
)

interface CarLoginRepository {

    suspend fun find(code: String): ApiResult<CarLoginRequest>

    suspend fun approve(code: String): ApiResult<Unit>

    suspend fun reject(code: String): ApiResult<Unit>
}

/** 차량 QR에서 로그인 코드를 꺼낸다. QR에는 ieo://car-login?code=KX7P-M4QZ 가 들어 있다 */
object CarLoginCode {

    private const val PREFIX = "ieo://car-login?code="
    private const val LENGTH = 8

    /** 차량 QR이 아니면 null */
    fun fromQr(content: String): String? {
        if (!content.startsWith(PREFIX)) return null
        return normalize(content.removePrefix(PREFIX).substringBefore('&')).takeIf(::isComplete)
    }

    /** 직접 입력한 값을 KX7P-M4QZ 모양으로 맞춘다 */
    fun normalize(input: String): String {
        val compact = input.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(LENGTH)
        return if (compact.length > 4) compact.take(4) + "-" + compact.drop(4) else compact
    }

    fun isComplete(code: String): Boolean = code.length == LENGTH + 1
}
