package com.ieo.mobile.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.ieo.mobile.domain.order.GeoPoint
import com.ieo.mobile.domain.order.LocationSource
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 근처 오더를 찾을 위치. 대략이면 충분해서 대략 위치 권한만 쓴다.
 * 폰이 최근에 안 위치가 있으면 그것을 쓰고, 없거나 오래됐으면 새로 한 번 묻는다.
 * 처음 켠 폰이나 다른 앱이 위치를 쓴 적 없는 폰은 "마지막 위치"가 비어 있어서, 묻지 않으면 계속 위치를 모른다
 */
class DeviceLocationSource(context: Context) : LocationSource {

    private val appContext = context.applicationContext

    override suspend fun lastKnown(): GeoPoint? {
        val granted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val manager = appContext.getSystemService(LocationManager::class.java) ?: return null
        val recent = runCatching {
            manager.getProviders(true)
                .mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull(Location::getTime)
        }.getOrNull()
        val fresh = recent?.takeIf { System.currentTimeMillis() - it.time < FRESH_MILLIS }
            ?: current(manager)
            ?: recent
        return fresh?.let { GeoPoint(it.latitude, it.longitude) }
    }

    /** 켜져 있는 위치 제공자에 지금 위치를 한 번 묻는다. 몇 초 안에 답이 없으면 null */
    private suspend fun current(manager: LocationManager): Location? {
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        for (provider in providers) {
            val location = withTimeoutOrNull(CURRENT_TIMEOUT_MILLIS) { ask(manager, provider) }
            if (location != null) return location
        }
        return null
    }

    private suspend fun ask(manager: LocationManager, provider: String): Location? = suspendCancellableCoroutine { cont ->
        val cancel = CancellationSignal()
        cont.invokeOnCancellation { cancel.cancel() }
        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, cancel, ContextCompat.getMainExecutor(appContext)) { location ->
                if (cont.isActive) cont.resume(location)
            }
        } catch (e: SecurityException) {
            // 대략 위치 권한으로는 GPS 제공자를 못 쓰는 기기가 있다
            if (cont.isActive) cont.resume(null)
        } catch (e: IllegalArgumentException) {
            if (cont.isActive) cont.resume(null)
        }
    }

    private companion object {
        /** 이보다 오래된 마지막 위치면 새로 묻는다 */
        const val FRESH_MILLIS = 10 * 60 * 1000L
        const val CURRENT_TIMEOUT_MILLIS = 5_000L
    }
}
