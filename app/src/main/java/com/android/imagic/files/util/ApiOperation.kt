package com.android.imagic.files.util

import com.android.imagic.files.provider.cloud.sdrive.client.ApiErrorCode
import com.android.imagic.files.provider.cloud.sdrive.client.exception.LoginExpiredException
import com.android.imagic.files.provider.cloud.sdrive.client.exception.UnknownApiException
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val result: Boolean,
    val message: String? = null,
    val statusCode: String,
    val data: T? = null
)


inline fun <T> safeApiCall(apiCall: () -> T): ApiOperation<T> {
    return try {
        ApiOperation.Success(data = apiCall())
    } catch (e: Exception) {
        ApiOperation.Failure(exception = e)
    }
}

inline fun <T> safeApiCallHandled(apiCall: () -> ApiResponse<T>): ApiOperation<T> {
    return try {
        val response = apiCall()

        if (response.result && response.data != null) {
            ApiOperation.Success(response.data)
        } else {
            val errorCode = ApiErrorCode.from(response.statusCode)
            val exception = when (errorCode) {
                ApiErrorCode.LOGIN_EXPIRED -> LoginExpiredException(response.message)
                else -> UnknownApiException(response.message, response.statusCode)
            }
            ApiOperation.Failure(exception)
        }
    } catch (e: Exception) {
        ApiOperation.Failure(e)
    }
}

sealed interface ApiOperation<T> {
    data class Success<T>(val data: T) : ApiOperation<T>
    data class Failure<T>(val exception: Exception) : ApiOperation<T>

    fun <R> mapSuccess(transform: (T) -> R): ApiOperation<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Failure -> Failure(exception)
        }
    }

    suspend fun onSuccess(block: suspend (T) -> Unit): ApiOperation<T> {
        if (this is Success) block(data)
        return this
    }

    fun onFailure(block: (Exception) -> Unit): ApiOperation<T> {
        if (this is Failure) block(exception)
        return this
    }
}
