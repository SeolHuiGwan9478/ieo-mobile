package com.ieo.mobile.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ieo.mobile.IeoApplication
import com.ieo.mobile.MainActivity
import com.ieo.mobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * FCM 받기 (09 알림). 서버가 data 메시지로 보내므로 앱이 꺼져 있어도 여기서 알림을 그린다.
 * 누르면 MainActivity가 열리고, [PushIntent]의 값으로 그 알림의 화면(단골 주선사, 내 배차 등)으로 간다
 */
class IeoMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val container = (application as IeoApplication).container
        if (!container.session.isLoggedIn.value) return
        scope.launch { container.pushRegistrar.upload(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val body = data["body"] ?: return
        val notificationId = data["notificationId"]?.toLongOrNull() ?: System.currentTimeMillis()
        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            PushIntent.put(this, data)
        }
        val pending = PendingIntent.getActivity(
            this, notificationId.toInt(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground_white)
            .setContentTitle(data["title"] ?: getString(R.string.app_name))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching { NotificationManagerCompat.from(this).notify(notificationId.toInt(), notification) }
    }

    companion object {
        const val CHANNEL_ID = "ieo_alerts"

        /** 앱이 켜질 때 한 번. 안드로이드 8 이상은 채널이 있어야 알림이 뜬다 */
        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "배차·단골 알림", NotificationManager.IMPORTANCE_HIGH)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
