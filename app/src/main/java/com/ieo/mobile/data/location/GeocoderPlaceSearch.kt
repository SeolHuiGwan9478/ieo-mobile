package com.ieo.mobile.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.ieo.mobile.domain.order.LocationSource
import com.ieo.mobile.domain.trip.PlaceSearch
import com.ieo.mobile.domain.trip.StopChoice
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 폰에 들어 있는 주소 찾기(Geocoder)를 쓴다. 지도 API 키 없이 동작한다 */
class GeocoderPlaceSearch(context: Context, private val location: LocationSource) : PlaceSearch {

    private val geocoder = Geocoder(context.applicationContext, Locale.KOREA)

    @Suppress("DEPRECATION")
    override suspend fun search(query: String): List<StopChoice.New> = withContext(Dispatchers.IO) {
        runCatching { geocoder.getFromLocationName(query, MAX_RESULTS).orEmpty().map { it.toChoice(query) } }.getOrDefault(emptyList())
    }

    @Suppress("DEPRECATION")
    override suspend fun here(): StopChoice.New? {
        val point = location.lastKnown() ?: return null
        return withContext(Dispatchers.IO) {
            runCatching { geocoder.getFromLocation(point.latitude, point.longitude, 1)?.firstOrNull()?.toChoice(null) }.getOrNull()
        }
    }

    private fun Address.toChoice(query: String?): StopChoice.New {
        val full = getAddressLine(0).orEmpty().removePrefix("대한민국 ")
        val short = query?.trim()?.take(MAX_NAME) ?: full.split(" ").takeLast(2).joinToString(" ")
        return StopChoice.New(short, full, latitude, longitude)
    }

    private companion object {
        const val MAX_RESULTS = 5
        const val MAX_NAME = 30
    }
}
