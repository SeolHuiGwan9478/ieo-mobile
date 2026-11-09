package com.ieo.mobile.domain.vehicle

/** 트럭 폭·길이(cm), 총중량(kg), 축 수. 선택 입력이고 비어 있으면 톤수 기준으로 경로와 통행료를 계산한다 */
data class VehicleDimensions(
    val widthCm: Int? = null,
    val lengthCm: Int? = null,
    val totalWeightKg: Int? = null,
    val axleCount: Int? = null,
) {
    companion object {
        val WIDTH = DimensionRange(100, 300, 10, 250)
        val LENGTH = DimensionRange(200, 2_000, 50, 850)
        val TOTAL_WEIGHT = DimensionRange(1_000, 40_000, 1_000, 11_000)
        val AXLES = DimensionRange(2, 6, 1, 2)
    }
}

/** 처음 누르면 [start]에서 시작하고, 이후 [step]씩 바꾼다. 범위 밖으로 나가지 않는다 */
data class DimensionRange(val min: Int, val max: Int, val step: Int, val start: Int) {
    fun next(current: Int?, delta: Int): Int = if (current == null) start else (current + delta).coerceIn(min, max)
}
