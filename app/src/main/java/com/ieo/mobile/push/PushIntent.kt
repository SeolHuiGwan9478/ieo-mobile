package com.ieo.mobile.push

import android.content.Intent
import com.ieo.mobile.domain.notification.AppNotification
import com.ieo.mobile.domain.notification.NotificationType
import java.time.Instant

/** 푸시 알림을 눌러 열린 MainActivity에 그 알림을 실어 보낸다 */
object PushIntent {

    private const val PREFIX = "ieo.push."
    private val KEYS = listOf("type", "notificationId", "orderId", "driverLinkId", "createdAt", "body")

    fun put(intent: Intent, data: Map<String, String>) {
        KEYS.forEach { key -> data[key]?.let { intent.putExtra(PREFIX + key, it) } }
    }

    /** 푸시로 열린 게 아니거나 모르는 종류면 null */
    fun read(intent: Intent?): AppNotification? {
        intent ?: return null
        fun get(key: String) = intent.getStringExtra(PREFIX + key)
        val type = NotificationType.entries.firstOrNull { it.name == get("type") } ?: return null
        return AppNotification(
            id = get("notificationId")?.toLongOrNull() ?: return null,
            type = type,
            message = get("body").orEmpty(),
            orderId = get("orderId")?.toLongOrNull(),
            createdAt = get("createdAt")?.let(Instant::parse) ?: Instant.now(),
            driverLinkId = get("driverLinkId")?.toLongOrNull(),
        )
    }

    /** 한 번 읽은 푸시는 화면을 돌려도 다시 열지 않게 지운다 */
    fun clear(intent: Intent?) {
        KEYS.forEach { intent?.removeExtra(PREFIX + it) }
    }
}
