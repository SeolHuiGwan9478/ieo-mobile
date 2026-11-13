package com.ieo.mobile

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.ieo.mobile.push.IeoMessagingService
import com.ieo.mobile.di.AppContainer

class IeoApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        startPush()
    }

    /**
     * 휴대폰 푸시(FCM). google-services.json으로 빌드했을 때만 Firebase를 켠다 (값은 BuildConfig).
     * 없으면 꺼 두고 앱 안 알림(SSE)만 받는다
     */
    private fun startPush() {
        if (BuildConfig.FIREBASE_APP_ID.isBlank()) return
        val options = FirebaseOptions.Builder()
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
            .build()
        if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, options)
        IeoMessagingService.createChannel(this)
    }
}
