package com.ieo.mobile.domain.notification

import com.ieo.mobile.core.network.ApiResult
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** 서버가 차주에게 보낸 알림 한 건 (09 알림). 지난 알림을 지워도 되게 읽음은 기기에만 남긴다 */
data class AppNotification(
    val id: Long,
    val type: NotificationType,
    val message: String,
    val orderId: Long,
    val createdAt: Instant,
)

/** [label]은 색 없이도 뜻이 읽히게 붙이는 말, [action]은 눌렀을 때 가는 곳의 이름 */
enum class NotificationType(val label: String, val action: String) {
    DISPATCH_CONFIRMED("배차 확정", "내 배차 보기"),
    DISPATCH_DECLINED("마감", "다른 오더 찾기"),
    DISPATCH_EXPIRED("시간 초과", "다른 오더 찾기"),
}

sealed interface NotificationStreamEvent {
    data object Connected : NotificationStreamEvent

    data class Received(val notification: AppNotification) : NotificationStreamEvent
}

interface NotificationRepository {

    /** 최근 알림. 오래된 순서 */
    suspend fun recent(): ApiResult<List<AppNotification>>

    /** [after] 다음 알림부터 받는다. 서버가 닫으면 끝나고, 연결이 실패하면 예외로 끝난다 */
    fun stream(after: Long?): Flow<NotificationStreamEvent>
}

/** 어디까지 봤는지. 이 기기에만 남긴다 */
interface NotificationSeenStore {
    var lastSeenId: Long?
}
