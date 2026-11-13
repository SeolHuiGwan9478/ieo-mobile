package com.ieo.mobile.domain.link

import com.ieo.mobile.core.network.ApiResult
import java.time.Instant

/**
 * 단골 주선사 연결 (BRK-002). 주선사가 내 번호로 요청하고, 내가 수락하면 연결된다.
 * 수락은 "그 회사 일을 하는 동안 위치·도착 예정 시각을 그 회사에 공유"하는 동의다.
 * 연결된 회사는 나에게만 보이는 지정 배차를 보낼 수 있다.
 */
data class DriverLink(
    val id: Long,
    val brokerName: String,
    val status: DriverLinkStatus,
    /** 받은 요청(PENDING)이면 이때까지 수락할 수 있다 */
    val expiresAt: Instant?,
    /** 연결됨(LINKED)이면 수락한 때 */
    val linkedAt: Instant?,
    /** 그 회사의 신뢰 지표 (지급 평균 등). 고를 근거로 보여 준다 */
    val brokerMetrics: com.ieo.mobile.domain.order.BrokerMetrics? = null,
    /** 이 회사와 끝낸 운행 수 */
    val tripsTogether: Long = 0,
)

/** 서버 DriverLinkStatus와 같은 값 */
enum class DriverLinkStatus { PENDING, LINKED, DECLINED, CANCELED, UNLINKED, EXPIRED }

interface DriverLinkRepository {

    /** 내 번호로 온 요청 (대기 중인 것만) */
    suspend fun invitations(): ApiResult<List<DriverLink>>

    /** 연결된 회사 */
    suspend fun links(): ApiResult<List<DriverLink>>

    suspend fun accept(linkId: Long): ApiResult<DriverLink>

    suspend fun decline(linkId: Long): ApiResult<Unit>

    /** 해제하면 위치 공유와 새 지정 배차가 끝난다. 이미 잡은 배차는 그대로 */
    suspend fun unlink(linkId: Long): ApiResult<Unit>
}
