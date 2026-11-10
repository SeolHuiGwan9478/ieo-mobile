package com.ieo.mobile.data.vehicle

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.map
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.vehicle.BodyType
import com.ieo.mobile.domain.vehicle.NewVehicle
import com.ieo.mobile.domain.vehicle.Tonnage
import com.ieo.mobile.domain.vehicle.Vehicle
import com.ieo.mobile.domain.vehicle.VehicleDimensions
import com.ieo.mobile.domain.vehicle.VehicleRepository
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface VehicleApi {

    @GET("api/vehicles")
    suspend fun list(): Response<List<VehicleResponse>>

    @POST("api/vehicles")
    suspend fun add(@Body body: CreateVehicleRequest): Response<VehicleResponse>

    @PATCH("api/vehicles/{id}")
    suspend fun update(@Path("id") id: Long, @Body body: UpdateVehicleRequest): Response<VehicleResponse>
}

@Serializable
data class CreateVehicleRequest(
    val plateNumber: String,
    val tonnage: String,
    val bodyType: String,
    val heightM: Double? = null,
    val widthCm: Int? = null,
    val lengthCm: Int? = null,
    val totalWeightKg: Int? = null,
    val axleCount: Int? = null,
)

/** 높이를 빼고 보내면 서버가 높이를 비운다 */
@Serializable
data class UpdateVehicleRequest(
    val tonnage: String,
    val bodyType: String,
    val heightM: Double? = null,
    val widthCm: Int? = null,
    val lengthCm: Int? = null,
    val totalWeightKg: Int? = null,
    val axleCount: Int? = null,
    val plateNumber: String? = null,
)

@Serializable
data class VehicleResponse(
    val id: Long,
    val plateNumber: String,
    val tonnage: String,
    val bodyType: String,
    val heightM: Double? = null,
    val defaultVehicle: Boolean = false,
    val pleosLinked: Boolean = false,
    val widthCm: Int? = null,
    val lengthCm: Int? = null,
    val totalWeightKg: Int? = null,
    val axleCount: Int? = null,
) {
    fun toDomain() = Vehicle(
        id = id,
        plateNumber = plateNumber,
        tonnage = Tonnage.valueOf(tonnage),
        bodyType = BodyType.valueOf(bodyType),
        heightMeters = heightM,
        isDefault = defaultVehicle,
        isLinkedToCar = pleosLinked,
        dimensions = VehicleDimensions(widthCm, lengthCm, totalWeightKg, axleCount),
    )
}

class DefaultVehicleRepository(
    private val api: VehicleApi,
    private val caller: ApiCaller,
) : VehicleRepository {

    override suspend fun list(): ApiResult<List<Vehicle>> =
        caller.call { api.list() }.mapResponse { list -> list.map(VehicleResponse::toDomain) }

    override suspend fun add(vehicle: NewVehicle): ApiResult<Unit> = caller.callOptional {
        api.add(CreateVehicleRequest(vehicle.plateNumber, vehicle.tonnage.name, vehicle.bodyType.name, vehicle.heightMeters,
            vehicle.dimensions.widthCm, vehicle.dimensions.lengthCm, vehicle.dimensions.totalWeightKg, vehicle.dimensions.axleCount))
    }.map { }

    override suspend fun update(
        vehicleId: Long,
        tonnage: Tonnage,
        bodyType: BodyType,
        heightMeters: Double?,
        dimensions: VehicleDimensions,
        plateNumber: String?,
    ): ApiResult<Unit> = caller.callOptional {
        api.update(
            vehicleId,
            UpdateVehicleRequest(tonnage.name, bodyType.name, heightMeters, dimensions.widthCm, dimensions.lengthCm, dimensions.totalWeightKg, dimensions.axleCount, plateNumber),
        )
    }.map { }

}
