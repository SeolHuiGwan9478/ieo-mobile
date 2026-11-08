package com.ieo.mobile.domain.order

data class GeoPoint(val latitude: Double, val longitude: Double)

/** 지금 위치. 권한이 없거나 모르면 null */
fun interface LocationSource {
    suspend fun lastKnown(): GeoPoint?
}
