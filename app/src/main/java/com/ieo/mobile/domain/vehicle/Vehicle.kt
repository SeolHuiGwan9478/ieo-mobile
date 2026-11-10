package com.ieo.mobile.domain.vehicle

import com.ieo.mobile.core.network.ApiResult
import kotlin.math.roundToInt

/**
 * 서버 Tonnage와 같은 이름. [defaultKmPerLiter]는 서버와 같은 톤수별 기본 연비(km/L)이고,
 * 차주가 평소 연비를 비워 둘 때 무엇을 쓰는지 보여 주는 데 쓴다
 */
enum class Tonnage(val label: String, val defaultKmPerLiter: Double) {
    TON_1("1톤", 8.5),
    TON_1_4("1.4톤", 8.0),
    TON_2_5("2.5톤", 7.5),
    TON_3_5("3.5톤", 6.5),
    TON_5("5톤", 5.0),
    TON_5_AXLE("5톤 축", 4.7),
    TON_8("8톤", 4.5),
    TON_11("11톤", 4.2),
    TON_14("14톤", 4.0),
    TON_18("18톤", 3.6),
    TON_25("25톤", 3.3),
}

/** 서버 BodyType과 같은 이름 */
enum class BodyType(val label: String) {
    CARGO("카고"),
    WING("윙바디"),
    BOX("탑차"),
    REFRIGERATED("냉동·냉장"),
    LIFT("리프트"),
    ETC("기타"),
}

/** 새로 등록할 차량. 높이를 모르면 null */
data class NewVehicle(
    val plateNumber: String,
    val tonnage: Tonnage,
    val bodyType: BodyType,
    val heightMeters: Double? = null,
    val dimensions: VehicleDimensions = VehicleDimensions(),
)

data class Vehicle(
    val id: Long,
    val plateNumber: String,
    val tonnage: Tonnage,
    val bodyType: BodyType,
    val heightMeters: Double?,
    val isDefault: Boolean,
    /** 차량 앱에서 이 트럭과 연결됐다 (VEH-004) */
    val isLinkedToCar: Boolean = false,
    val dimensions: VehicleDimensions = VehicleDimensions(),
    /** 차주가 넣은 평소 연비(km/L). 없으면 null */
    val manualKmPerLiter: Double? = null,
    /** Pleos 차량이 잰 연비(km/L). 없으면 null */
    val measuredKmPerLiter: Double? = null,
) {
    val label: String get() = "${tonnage.label} ${bodyType.label}"

    /** "5톤 카고 · 높이 3.6m" */
    val summary: String get() = listOf(label, VehicleHeight.label(heightMeters)).joinToString(" · ")
}

/** 차량 높이 (VEH-002). 키보드 없이 ±0.1m로 고친다 */
object VehicleHeight {
    const val MIN = 1.5
    const val MAX = 4.2
    private const val DEFAULT = 3.5

    /** 모름(null)에서 처음 누르면 3.5m에서 시작한다. 범위 안으로 자른다 */
    fun step(height: Double?, delta: Double): Double {
        val current = height ?: return DEFAULT
        return (((current + delta) * 10).roundToInt() / 10.0).coerceIn(MIN, MAX)
    }

    fun label(height: Double?): String = height?.let { "높이 %.1fm".format(it) } ?: "높이 모름"
}

interface VehicleRepository {

    suspend fun list(): ApiResult<List<Vehicle>>

    suspend fun add(vehicle: NewVehicle): ApiResult<Unit>

    /** [plateNumber]가 null이면 번호는 그대로. 번호·톤수·차종이 바뀌면 서버가 자동차등록증을 다시 받는다 */
    suspend fun update(
        vehicleId: Long,
        tonnage: Tonnage,
        bodyType: BodyType,
        heightMeters: Double?,
        dimensions: VehicleDimensions = VehicleDimensions(),
        plateNumber: String? = null,
    ): ApiResult<Unit>

    /** 평소 연비(km/L). null이면 비워서 톤수 기본값을 쓰게 한다 (VEH-003) */
    suspend fun setManualFuelEconomy(vehicleId: Long, kmPerLiter: Double?): ApiResult<Unit>
}
