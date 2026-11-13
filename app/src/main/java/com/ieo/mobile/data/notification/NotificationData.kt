package com.ieo.mobile.data.notification

import android.content.Context
import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.notification.AppNotification
import com.ieo.mobile.domain.notification.NotificationRepository
import com.ieo.mobile.domain.notification.NotificationSeenStore
import com.ieo.mobile.domain.notification.NotificationStreamEvent
import com.ieo.mobile.domain.notification.NotificationType
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import retrofit2.http.GET

interface NotificationApi {

    @GET("api/notifications")
    suspend fun recent(): retrofit2.Response<List<NotificationBody>>

    /** 로그인한 휴대폰의 FCM 토큰 (09 알림) */
    @retrofit2.http.PUT("api/notifications/push-token")
    suspend fun registerPushToken(@retrofit2.http.Body body: PushTokenBody): retrofit2.Response<Unit>

    /** 로그아웃할 때. 이 휴대폰으로 더는 푸시를 보내지 않는다 */
    @retrofit2.http.POST("api/notifications/push-token/remove")
    suspend fun removePushToken(@retrofit2.http.Body body: PushTokenBody): retrofit2.Response<Unit>
}

@Serializable
data class PushTokenBody(val token: String)

@Serializable
data class NotificationBody(
    val id: Long,
    val type: String,
    val message: String,
    val orderId: Long? = null,
    val driverLinkId: Long? = null,
    val createdAt: String,
) {
    /** 앱이 모르는 종류(서버가 새로 늘린 것)는 건너뛴다 */
    fun toDomain(): AppNotification? {
        val known = NotificationType.entries.firstOrNull { it.name == type } ?: return null
        return AppNotification(id, known, message, orderId, Instant.parse(createdAt), driverLinkId)
    }
}

/** 서버가 20초마다 빈 줄을 보내므로, 1분 동안 아무것도 오지 않으면 끊긴 것으로 본다 */
class DefaultNotificationRepository(
    private val api: NotificationApi,
    private val caller: ApiCaller,
    client: OkHttpClient,
    private val baseUrl: String,
    private val json: Json,
) : NotificationRepository {

    private val factory = EventSources.createFactory(client.newBuilder().readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS).build())

    override suspend fun recent(): ApiResult<List<AppNotification>> =
        caller.call { api.recent() }.mapResponse { list -> list.mapNotNull { it.toDomain() } }

    override fun stream(after: Long?): Flow<NotificationStreamEvent> = callbackFlow {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/notifications/stream" + (after?.let { "?after=$it" } ?: ""))
            .header("Accept", "text/event-stream, application/json")
            .build()
        val source = factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                trySend(NotificationStreamEvent.Connected)
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (type != EVENT_NAME) return
                val notification = runCatching { json.decodeFromString<NotificationBody>(data).toDomain() }.getOrNull() ?: return
                trySend(NotificationStreamEvent.Received(notification))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: IOException("알림 연결 실패: HTTP ${response?.code}"))
            }
        })
        awaitClose { source.cancel() }
    }

    private companion object {
        const val EVENT_NAME = "notification"
        const val READ_TIMEOUT_SECONDS = 60L
    }
}

class PrefsNotificationSeenStore(context: Context) : NotificationSeenStore {

    private val prefs = context.getSharedPreferences("ieo_notifications", Context.MODE_PRIVATE)

    override var lastSeenId: Long?
        get() = if (prefs.contains(KEY)) prefs.getLong(KEY, 0) else null
        set(value) {
            prefs.edit().apply { if (value == null) remove(KEY) else putLong(KEY, value) }.apply()
        }

    private companion object {
        const val KEY = "last_seen_id"
    }
}
