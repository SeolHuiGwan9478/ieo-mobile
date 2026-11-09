package com.ieo.mobile.domain.vehicle

/** 트럭 폭·길이(cm)와 총중량(kg). 선택 입력이고 비어 있으면 톤수별 기본값으로 경로를 찾는다 */
data class VehicleDimensions(
    val widthCm: Int? = null,
    val lengthCm: Int? = null,
    val totalWeightKg: Int? = null,
) {
    companion object {
        val WIDTH = DimensionRange(100, 300, 10, 250)
        val LENGTH = DimensionRange(200, 2_000, 50, 850)
        val TOTAL_WEIGHT = DimensionRange(1_000, 40_000, 1_000, 11_000)
    }
}

/** 처음 누르면 [start]에서 시작하고, 이후 [step]씩 바꾼다. 범위 밖으로 나가지 않는다 */
data class DimensionRange(val min: Int, val max: Int, val step: Int, val start: Int) {
    fun next(current: Int?, delta: Int): Int = if (current == null) start else (current + delta).coerceIn(min, max)
}
