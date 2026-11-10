package com.ieo.mobile.core.network

/** 실패 이유. [message]는 화면에 그대로 보여 줘도 되는 한국어 문장이다 */
sealed interface ApiError {

    val message: String

    /**
     * 서버가 오류 응답을 준 경우. 서버의 `{ code, message, fieldErrors }`를 그대로 담는다.
     * [code]는 서버 ErrorCode의 코드(예: AUTH_400_CODE)라서 경우별 처리에 쓴다.
     */
    data class Server(
        val status: Int,
        val code: String,
        override val message: String,
    ) : ApiError

    /** 서버에 닿지 못함 (인터넷 끊김, 서버 꺼짐, 시간 초과) */
    data object Network : ApiError {
        override val message = "서버에 연결하지 못했어요. 인터넷 연결을 확인해 주세요."
    }

    /** 응답을 읽지 못함 (앱과 서버의 응답 모양이 다를 때) */
    data object Unexpected : ApiError {
        override val message = "잠시 후 다시 시도해 주세요."
    }
}
