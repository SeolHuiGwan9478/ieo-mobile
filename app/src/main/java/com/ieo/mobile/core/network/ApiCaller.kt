package com.ieo.mobile.core.network

import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response

/**
 * Retrofit 호출을 [ApiResult]로 바꾼다. 저장소(Repository)는 모든 요청을 이걸로 감싼다.
 * 네트워크 예외와 서버 오류 응답을 한곳에서 처리하므로, 저장소마다 try/catch를 쓰지 않는다.
 */
class ApiCaller(private val json: Json) {

    /** 응답 본문이 꼭 있는 요청 */
    suspend fun <T : Any> call(request: suspend () -> Response<T>): ApiResult<T> =
        when (val result = callOptional(request)) {
            is ApiResult.Success -> result.data?.let { ApiResult.Success(it) } ?: ApiResult.Failure(ApiError.Unexpected)
            is ApiResult.Failure -> result
        }

    /** 본문이 없을 수 있는 요청 (예: 진행 중인 운행이 없으면 204) */
    suspend fun <T : Any> callOptional(request: suspend () -> Response<T>): ApiResult<T?> = try {
        val response = request()
        if (response.isSuccessful) ApiResult.Success(response.body()) else ApiResult.Failure(parseError(response))
    } catch (e: IOException) {
        ApiResult.Failure(ApiError.Network)
    } catch (e: HttpException) {
        ApiResult.Failure(ApiError.Unexpected)
    } catch (e: SerializationException) {
        ApiResult.Failure(ApiError.Unexpected)
    }

    /** 성공했는지만 보면 되는 요청 (204) */
    suspend fun callNoContent(request: suspend () -> Response<Unit>): ApiResult<Unit> =
        callOptional(request).map { }

    private fun parseError(response: Response<*>): ApiError {
        val body = runCatching { response.errorBody()?.string() }.getOrNull()
        val parsed = body?.let { runCatching { json.decodeFromString<ErrorBody>(it) }.getOrNull() }
        return ApiError.Server(
            status = response.code(),
            code = parsed?.code ?: "HTTP_${response.code()}",
            message = parsed?.message ?: ApiError.Unexpected.message,
        )
    }

    /** 서버 global/error/ErrorResponse와 같은 모양 */
    @Serializable
    private data class ErrorBody(val code: String, val message: String)
}
