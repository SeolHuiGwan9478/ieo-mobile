package com.ieo.mobile

import android.app.Application
import com.ieo.mobile.di.AppContainer

class IeoApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
