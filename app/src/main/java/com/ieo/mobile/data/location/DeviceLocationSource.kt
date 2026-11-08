package com.ieo.mobile.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.ieo.mobile.domain.order.GeoPoint
import com.ieo.mobile.domain.order.LocationSource

/** 폰이 마지막으로 안 위치를 쓴다. 근처 오더를 찾는 데만 쓰므로 대략이면 충분하다 */
class DeviceLocationSource(context: Context) : LocationSource {

    private val appContext = context.applicationContext

    override suspend fun lastKnown(): GeoPoint? {
        val granted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val manager = appContext.getSystemService(LocationManager::class.java) ?: return null
        return runCatching {
            manager.getProviders(true)
                .mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull(Location::getTime)
                ?.let { GeoPoint(it.latitude, it.longitude) }
        }.getOrNull()
    }
}
