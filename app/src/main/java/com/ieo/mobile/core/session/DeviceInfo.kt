package com.ieo.mobile.core.session

import android.content.Context
import android.os.Build
import java.util.UUID

/**
 * 서버에 알리는 이 기기 정보. deviceId는 앱 설치마다 한 번 만든다.
 * 서버는 휴대폰 인증 결과(phoneToken)를 이 deviceId에만 묶어 둔다.
 */
class DeviceInfo(context: Context, val appVersion: String) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val deviceId: String by lazy {
        prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }
    }

    /** QR 승인 화면에 "어느 기기에서 로그인하려는지" 보여 줄 이름 */
    val deviceName: String = listOf(Build.MANUFACTURER, Build.MODEL)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .take(50)

    private companion object {
        const val PREFS_NAME = "ieo_device"
        const val KEY_DEVICE_ID = "device_id"
    }
}
