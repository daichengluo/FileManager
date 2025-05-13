package com.android.imagic.files.appwidget

import com.android.imagic.files.app.networkJson
import com.android.imagic.files.app.okHttpClient
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.safeApiCall
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

enum class QrcodeLoginStatus {
    UNSTARTED, WAITING, LOGGING, SUCCEED, FAILED
}

internal interface AuthClient {
    suspend fun getState(state: String): ApiOperation<QrcodeLoginStatus>
    suspend fun getAccessToken(state: String): ApiOperation<AccessTokenData?>
}

@Serializable
data class AccessTokenData(
    val id: Int,
    val mac: String,
    val access_token: String,
    val email: String,
    val refresh_token: String,
    val sub: String,
    val givenname: String?,
    val familyname: String,
    val picture: String,
    val expires_in: Long,
    val ext_expires_in: Long,
    val state: String
)

object AuthClientImpl : AuthClient {

    private const val BASE_URL = "https://wboard.starboard-online.com/api/ApiIgc/"
    private val json: Json = networkJson
    private val client = okHttpClient.newBuilder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    private val network = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(AuthApi::class.java)

    override suspend fun getState(state: String): ApiOperation<QrcodeLoginStatus> {
        return try {
            val response = network.getState(state)
            when {
                response.result -> ApiOperation.Success(QrcodeLoginStatus.LOGGING)
                response.statusCode == "201" -> ApiOperation.Success(QrcodeLoginStatus.WAITING)
                else -> ApiOperation.Success(QrcodeLoginStatus.FAILED)
            }
        } catch (e: Exception) {
            ApiOperation.Failure(e)
        }
    }

    override suspend fun getAccessToken(state: String): ApiOperation<AccessTokenData?> =
        safeApiCall { network.getAccessToken(state).data }

}
