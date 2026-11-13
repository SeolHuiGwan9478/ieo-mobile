package com.ieo.mobile.data

import com.ieo.mobile.data.notification.NotificationBody
import com.ieo.mobile.domain.notification.NotificationType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 서버 알림 JSON이 그대로 읽히는지. 단골 요청 알림은 오더 없이 요청 id만 온다 (BRK-002) */
class NotificationDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `단골 요청 알림은 오더 없이 요청 id로 읽는다`() {
        val body = """{"id":7,"type":"DRIVER_LINK_REQUESTED","message":"한빛로지스에서 단골 연결을 요청했어요. 휴대폰 앱에서 수락하거나 거절할 수 있어요",
            "dispatchRequestId":null,"orderId":null,"driverLinkId":3,"createdAt":"2026-11-13T05:00:00Z"}"""
        val notification = json.decodeFromString<NotificationBody>(body).toDomain()!!
        assertEquals(NotificationType.DRIVER_LINK_REQUESTED, notification.type)
        assertEquals(3L, notification.driverLinkId)
        assertNull(notification.orderId)
    }

    @Test
    fun `배차 알림은 오더 id로 읽는다`() {
        val body = """{"id":8,"type":"DISPATCH_CONFIRMED","message":"확정","dispatchRequestId":1,"orderId":11,"driverLinkId":null,"createdAt":"2026-11-13T05:00:00Z"}"""
        val notification = json.decodeFromString<NotificationBody>(body).toDomain()!!
        assertEquals(11L, notification.orderId)
        assertNull(notification.driverLinkId)
    }
}
