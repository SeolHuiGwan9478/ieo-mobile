package com.ieo.mobile.push

import android.content.Intent
import com.ieo.mobile.domain.notification.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 푸시 data → 알림을 눌러 열린 화면이 읽는 알림 (09 알림, BRK-002) */
@RunWith(RobolectricTestRunner::class)
class PushIntentTest {

    @Test
    fun `단골 요청 푸시를 눌러 열면 그 알림으로 읽고, 한 번 읽으면 지운다`() {
        val intent = Intent()
        PushIntent.put(intent, mapOf("type" to "DRIVER_LINK_REQUESTED", "notificationId" to "7", "driverLinkId" to "3",
            "createdAt" to "2026-11-13T05:00:00Z", "body" to "한빛로지스에서 단골 연결을 요청했어요", "title" to "이에오"))

        val read = PushIntent.read(intent)!!
        assertEquals(NotificationType.DRIVER_LINK_REQUESTED, read.type)
        assertEquals(7L, read.id)
        assertEquals(3L, read.driverLinkId)
        assertNull(read.orderId)

        PushIntent.clear(intent)
        assertNull(PushIntent.read(intent))
    }

    @Test
    fun `모르는 종류나 푸시가 아닌 실행은 null`() {
        assertNull(PushIntent.read(Intent()))
        val unknown = Intent()
        PushIntent.put(unknown, mapOf("type" to "SOMETHING_NEW", "notificationId" to "1"))
        assertNull(PushIntent.read(unknown))
    }
}
