package com.ieo.mobile.core.network

/**
 * 서버 요청 결과. 예외를 던지지 않고 성공·실패를 값으로 돌려준다.
 * 화면(ViewModel)은 when으로 두 경우를 모두 처리한다.
 */
sealed interface ApiResult<out T> {

    data class Success<out T>(val data: T) : ApiResult<T>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

inline fun <T> ApiResult<T>.onFailure(action: (ApiError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) action(error)
    return this
}

/**
 * 서버 응답(DTO)을 앱 모델로 바꿀 때 쓴다. 모르는 enum 값이나 잘못된 날짜가 와도
 * 앱이 죽지 않고 [ApiError.Unexpected]로 끝난다.
 */
inline fun <T, R> ApiResult<T>.mapResponse(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> try {
        ApiResult.Success(transform(data))
    } catch (e: IllegalArgumentException) {
        ApiResult.Failure(ApiError.Unexpected)
    } catch (e: java.time.DateTimeException) {
        ApiResult.Failure(ApiError.Unexpected)
    }
    is ApiResult.Failure -> this
}
