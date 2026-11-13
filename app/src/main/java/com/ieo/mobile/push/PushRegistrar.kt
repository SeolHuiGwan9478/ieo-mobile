package com.ieo.mobile.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.data.notification.NotificationApi
import com.ieo.mobile.data.notification.PushTokenBody
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 휴대폰 푸시(FCM) 토큰을 서버에 올리고 내린다 (09 알림).
 * google-services.json 없이 빌드했으면 Firebase가 없어 아무것도 하지 않는다. 그때는 앱 안 알림(SSE)만 받는다
 */
class PushRegistrar(context: Context, private val api: NotificationApi, private val caller: ApiCaller) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("push", Context.MODE_PRIVATE)

    /** Firebase를 켜고 빌드했는지 */
    val available: Boolean get() = FirebaseApp.getApps(appContext).isNotEmpty()

    /** 로그인했을 때. 이 휴대폰의 토큰을 받아 올린다 */
    suspend fun register() {
        if (!available) return
        currentToken()?.let { upload(it) }
    }

    /** FCM이 토큰을 바꿨을 때 (IeoMessagingService) */
    suspend fun upload(token: String) {
        caller.callNoContent { api.registerPushToken(PushTokenBody(token)) }
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    /** 로그아웃하기 전에. 로그인 토큰이 남아 있을 때 불러야 서버가 받는다 */
    suspend fun unregister() {
        val token = prefs.getString(KEY_TOKEN, null) ?: return
        caller.callNoContent { api.removePushToken(PushTokenBody(token)) }
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private suspend fun currentToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            continuation.resume(if (task.isSuccessful) task.result else null)
        }
    }

    private companion object {
        const val KEY_TOKEN = "fcm_token"
    }
}
