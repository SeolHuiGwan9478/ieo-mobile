package com.ieo.mobile.data.document

import com.ieo.mobile.core.network.ApiCaller
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.core.network.mapResponse
import com.ieo.mobile.domain.document.BusinessInfo
import com.ieo.mobile.domain.document.DocumentItem
import com.ieo.mobile.domain.document.DocumentRepository
import com.ieo.mobile.domain.document.DocumentStatus
import com.ieo.mobile.domain.document.DocumentSubmission
import com.ieo.mobile.domain.document.DriverDocuments
import com.ieo.mobile.domain.document.DriverRecord
import java.time.Instant
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** 차주 서류 제출 (서버 07 인계서 7장). 심사는 운영자가 한다 */
interface DocumentApi {

    @GET("api/drivers/me/documents")
    suspend fun documents(): Response<DriverDocumentsBody>

    @GET("api/drivers/me/record")
    suspend fun record(): Response<DriverRecordBody>

    @Multipart
    @POST("api/drivers/me/documents")
    suspend fun submit(
        @Part("type") type: RequestBody,
        @Part file: MultipartBody.Part,
        @Part("businessNumber") businessNumber: RequestBody?,
        @Part("openedOn") openedOn: RequestBody?,
    ): Response<DocumentBody>
}

@Serializable
data class DocumentBody(
    val id: Long,
    val status: String,
    val submittedAt: String? = null,
    val rejectMessage: String? = null,
) {
    fun toDomain() = DocumentSubmission(id, DocumentStatus.valueOf(status), submittedAt?.let(Instant::parse), rejectMessage)
}

@Serializable
data class DocumentTypeBody(
    val type: String,
    val label: String,
    val required: Boolean,
    val checks: List<String> = emptyList(),
    val latest: DocumentBody? = null,
) {
    fun toDomain() = DocumentItem(type, label, required, checks, latest?.toDomain())
}

@Serializable
data class DriverDocumentsBody(
    val verified: Boolean,
    val approvedRequired: Int,
    val requiredTotal: Int,
    val documents: List<DocumentTypeBody>,
) {
    fun toDomain() = DriverDocuments(verified, approvedRequired, requiredTotal, documents.map { it.toDomain() })
}

@Serializable
data class DriverRecordBody(val verified: Boolean, val completedTripCount: Long = 0, val lateCancelCount: Long = 0) {
    fun toDomain() = DriverRecord(verified, completedTripCount, lateCancelCount)
}

class DefaultDocumentRepository(private val api: DocumentApi, private val caller: ApiCaller) : DocumentRepository {

    override suspend fun documents(): ApiResult<DriverDocuments> =
        caller.call { api.documents() }.mapResponse { it.toDomain() }

    override suspend fun record(): ApiResult<DriverRecord> =
        caller.call { api.record() }.mapResponse { it.toDomain() }

    override suspend fun submit(type: String, jpeg: ByteArray, business: BusinessInfo?): ApiResult<Unit> {
        val file = MultipartBody.Part.createFormData("file", "document.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
        val number = business?.businessNumber?.takeIf { it.isNotBlank() }?.let(::text)
        return caller.call { api.submit(text(type), file, number, business?.openedOn?.let(::text)) }.mapResponse { }
    }

    private fun text(value: String): RequestBody = value.toRequestBody("text/plain".toMediaType())
}
