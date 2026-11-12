package com.ieo.mobile.data.proof

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.proof.DeliveryProof
import com.ieo.mobile.domain.proof.DeliveryProofStatus
import com.ieo.mobile.domain.proof.ProofLink
import com.ieo.mobile.domain.proof.ProofRepository
import com.ieo.mobile.domain.proof.ReceiptPhoto
import com.ieo.mobile.domain.proof.WaitRecord
import com.ieo.mobile.domain.proof.WaitTimes
import com.ieo.mobile.domain.trip.StopType
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ProofApi {

    @GET("api/trips/{tripId}/waits")
    suspend fun waits(@Path("tripId") tripId: Long): Response<List<WaitRecordBody>>

    @POST("api/trips/{tripId}/wait-proof")
    suspend fun waitProof(@Path("tripId") tripId: Long): Response<ProofLinkBody>

    @PATCH("api/trips/{tripId}/stops/{stopType}/wait")
    suspend fun editWait(@Path("tripId") tripId: Long, @Path("stopType") stopType: String, @Body body: EditWaitBody): Response<WaitRecordBody>

    @GET("api/trips/{tripId}/delivery-proof")
    suspend fun deliveryProof(@Path("tripId") tripId: Long): Response<DeliveryProofBody>

    @Multipart
    @POST("api/trips/{tripId}/delivery-proof/photos")
    suspend fun uploadPhoto(@Path("tripId") tripId: Long, @Part file: MultipartBody.Part): Response<DeliveryProofBody>

    @DELETE("api/trips/{tripId}/delivery-proof/photos/{photoId}")
    suspend fun deletePhoto(@Path("tripId") tripId: Long, @Path("photoId") photoId: Long): Response<DeliveryProofBody>

    @POST("api/trips/{tripId}/delivery-proof/send")
    suspend fun send(@Path("tripId") tripId: Long): Response<DeliveryProofBody>

    @GET("api/trips/{tripId}/delivery-proof/photos/{photoId}")
    suspend fun photo(@Path("tripId") tripId: Long, @Path("photoId") photoId: Long): Response<ResponseBody>
}

@Serializable
data class WaitPolicyBody(val freeMinutes: Int? = null)

@Serializable
data class WaitRecordBody(
    val stopType: String,
    val siteName: String,
    val arrivedAt: String,
    val workStartedAt: String? = null,
    val departedAt: String? = null,
    val waitMinutes: Long? = null,
    val chargeableAmount: Long? = null,
    val policy: WaitPolicyBody? = null,
    val edited: Boolean = false,
) {
    fun toDomain() = WaitRecord(
        StopType.valueOf(stopType),
        siteName,
        Instant.parse(arrivedAt),
        workStartedAt?.let(Instant::parse),
        departedAt?.let(Instant::parse),
        waitMinutes,
        chargeableAmount,
        policy != null,
        edited,
    )
}

/** 서버 EditWaitRequest. 세 값을 모두 보낸다 (비운 것은 null) */
@Serializable
data class EditWaitBody(val arrivedAt: String, val workStartedAt: String?, val departedAt: String?)

@Serializable
data class ProofLinkBody(val path: String, val expiresAt: String) {
    fun toDomain(baseUrl: String) = ProofLink(baseUrl.trimEnd('/') + path, Instant.parse(expiresAt))
}

@Serializable
data class ProofPhotoBody(val id: Long)

@Serializable
data class DeliveryProofBody(
    val required: Boolean,
    val status: String,
    val sentAt: String? = null,
    val photos: List<ProofPhotoBody> = emptyList(),
    val maxPhotos: Int = 5,
    val canUpload: Boolean = false,
) {
    fun toDomain() = DeliveryProof(
        required,
        DeliveryProofStatus.valueOf(status),
        sentAt?.let(Instant::parse),
        photos.map { ReceiptPhoto(it.id) },
        maxPhotos,
        canUpload,
    )
}

/** [publicBaseUrl]: 주선사가 여는 대기 증빙 링크의 앞부분. 지금은 API 서버가 직접 보여 준다 */
class DefaultProofRepository(
    private val api: ProofApi,
    private val caller: ApiCaller,
    private val publicBaseUrl: String,
) : ProofRepository {

    override suspend fun waits(tripId: Long): ApiResult<List<WaitRecord>> =
        caller.call { api.waits(tripId) }.mapResponse { list -> list.map { it.toDomain() } }

    override suspend fun waitProofLink(tripId: Long): ApiResult<ProofLink> =
        caller.call { api.waitProof(tripId) }.mapResponse { it.toDomain(publicBaseUrl) }

    override suspend fun editWait(tripId: Long, stop: StopType, times: WaitTimes): ApiResult<WaitRecord> =
        caller.call {
            api.editWait(tripId, stop.name, EditWaitBody(times.arrivedAt.toString(), times.workStartedAt?.toString(), times.departedAt?.toString()))
        }.mapResponse { it.toDomain() }

    override suspend fun deliveryProof(tripId: Long): ApiResult<DeliveryProof> =
        caller.call { api.deliveryProof(tripId) }.mapResponse { it.toDomain() }

    override suspend fun uploadReceiptPhoto(tripId: Long, jpeg: ByteArray): ApiResult<DeliveryProof> {
        val part = MultipartBody.Part.createFormData("file", "paper.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
        return caller.call { api.uploadPhoto(tripId, part) }.mapResponse { it.toDomain() }
    }

    override suspend fun deleteReceiptPhoto(tripId: Long, photoId: Long): ApiResult<DeliveryProof> =
        caller.call { api.deletePhoto(tripId, photoId) }.mapResponse { it.toDomain() }

    override suspend fun sendReceipt(tripId: Long): ApiResult<DeliveryProof> =
        caller.call { api.send(tripId) }.mapResponse { it.toDomain() }

    override suspend fun receiptPhoto(tripId: Long, photoId: Long): ApiResult<ByteArray> =
        caller.call { api.photo(tripId, photoId) }.mapResponse { body -> withContext(Dispatchers.IO) { body.use { it.bytes() } } }
}
