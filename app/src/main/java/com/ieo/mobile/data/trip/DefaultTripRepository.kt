package com.ieo.mobile.data.trip

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.map
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.trip.CancelReason
import com.ieo.mobile.domain.trip.NewTrip
import com.ieo.mobile.domain.trip.StopChoice
import com.ieo.mobile.domain.trip.Trip
import com.ieo.mobile.domain.trip.TripRepository
import com.ieo.mobile.domain.trip.TripStatus
import com.ieo.mobile.domain.trip.TripStop
import com.ieo.mobile.domain.trip.VisitedSite
import java.time.Instant
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TripApi {

    @GET("api/trips")
    suspend fun list(@Query("size") size: Int): Response<List<TripResponse>>

    @POST("api/trips")
    suspend fun register(@Body body: CreateTripRequest): Response<TripResponse>

    @POST("api/trips/{id}/cancel")
    suspend fun cancel(@Path("id") id: Long, @Body body: CancelTripRequest): Response<TripResponse>
}

/** 서버 CancelTripRequest. 직접 등록한 운행은 사유를 비운다 */
@Serializable
data class CancelTripRequest(val reason: String? = null)

@Serializable
data class TripResponse(
    val id: Long,
    val status: String,
    val fare: Long,
    val paymentTypeLabel: String,
    val source: String,
    val brokerName: String? = null,
    val plateNumber: String,
    val pickup: TripStopResponse,
    val dropoff: TripStopResponse,
    val createdAt: String,
) {
    fun toDomain() = Trip(
        id = id,
        status = TripStatus.valueOf(status),
        fare = fare,
        paymentTypeLabel = paymentTypeLabel,
        brokerName = brokerName?.takeIf { it.isNotBlank() },
        plateNumber = plateNumber,
        pickup = pickup.toDomain(),
        dropoff = dropoff.toDomain(),
        isManual = source == MANUAL,
        createdAt = Instant.parse(createdAt),
    )

    private companion object {
        const val MANUAL = "MANUAL"
    }
}

@Serializable
data class TripStopResponse(val name: String, val address: String, val plannedAt: String? = null, val siteId: Long) {
    fun toDomain() = TripStop(name, address, plannedAt?.let(Instant::parse), siteId)
}

@Serializable
data class NewSiteRequest(val name: String, val address: String, val lat: Double, val lng: Double)

@Serializable
data class TripStopRequest(val siteId: Long? = null, val site: NewSiteRequest? = null, val plannedAt: String? = null) {
    companion object {
        fun from(choice: StopChoice, plannedAt: Instant?) = when (choice) {
            is StopChoice.Known -> TripStopRequest(siteId = choice.siteId, plannedAt = plannedAt?.toString())
            is StopChoice.New -> TripStopRequest(site = NewSiteRequest(choice.name, choice.address, choice.latitude, choice.longitude), plannedAt = plannedAt?.toString())
        }
    }
}

@Serializable
data class CreateTripRequest(
    val pickup: TripStopRequest,
    val dropoff: TripStopRequest,
    val fare: Long,
    val paymentType: String,
    val brokerName: String? = null,
)

class DefaultTripRepository(
    private val api: TripApi,
    private val caller: ApiCaller,
) : TripRepository {

    override suspend fun list(): ApiResult<List<Trip>> =
        caller.call { api.list(LIST_SIZE) }.mapResponse { list -> list.map(TripResponse::toDomain) }

    override suspend fun visitedSites(): ApiResult<List<VisitedSite>> = caller.call { api.list(LIST_SIZE) }.map { list ->
        list.flatMap { listOf(it.pickup, it.dropoff) }
            .distinctBy { it.siteId }
            .map { VisitedSite(it.siteId, it.name, it.address) }
    }

    override suspend fun register(trip: NewTrip): ApiResult<Unit> = caller.callOptional {
        api.register(
            CreateTripRequest(
                pickup = TripStopRequest.from(trip.pickup, trip.pickupAt),
                dropoff = TripStopRequest.from(trip.dropoff, null),
                fare = trip.fare,
                paymentType = trip.paymentType.name,
                brokerName = trip.brokerName,
            ),
        )
    }.map { }

    override suspend fun cancel(tripId: Long, reason: CancelReason?): ApiResult<Unit> =
        caller.callOptional { api.cancel(tripId, CancelTripRequest(reason?.name)) }.map { }

    private companion object {
        const val LIST_SIZE = 50
    }
}
