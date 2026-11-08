package com.ieo.mobile.domain.auth

/** 휴대폰 번호 표기 */
object PhoneFormat {

    /** 01012345678 → 010-1234-5678. 입력 중인 값도 자리에 맞게 끊는다 */
    fun hyphen(digits: String): String = when {
        digits.length <= 3 -> digits
        digits.length <= 7 -> digits.take(3) + "-" + digits.drop(3)
        digits.length <= 10 -> digits.take(3) + "-" + digits.substring(3, 6) + "-" + digits.drop(6)
        else -> digits.take(3) + "-" + digits.substring(3, 7) + "-" + digits.drop(7)
    }
}
