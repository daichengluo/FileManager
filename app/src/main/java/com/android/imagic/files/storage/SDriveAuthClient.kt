package com.android.imagic.files.storage

import com.android.imagic.files.BuildConfig
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

internal interface SDriveAuth {
    suspend fun getLoginUrlInfo(loginType: Int): ApiOperation<LoginUrlResponse>
    suspend fun doLogin(code: String, state: String): ApiOperation<LoginInfoResponse>
}

@Serializable
data class LoginUrlResponse(
    val result: Boolean,
    val message: String? = null,
    val statusCode: String,
    val data: LoginUrl?
) {
    @Serializable
    data class LoginUrl(
        val loginURL: String,
        val stateCode: String
    )
}

@Serializable
data class LoginInfoResponse(
    val result: Boolean,
    val message: String? = null,
    val statusCode: String,
    val data: LoginInfo?
) {
    @Serializable
    data class LoginInfo(
        val id: Int,
        val openid: String,
        val access_token: String,
        val login_type: String,
        val refresh_token: String?,
        val unionid: String,
        val name: String?,
        val nickname: String,
        val headimgurl: String
    )
}

object SDriveAuthClient : SDriveAuth {

    private val baseUrl: String = BuildConfig.STB_API_URL
    private val json: Json = networkJson
    private val client = okHttpClient.newBuilder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    private val network = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(SDriveAuthApi::class.java)

    override suspend fun getLoginUrlInfo(loginType: Int): ApiOperation<LoginUrlResponse> =
        safeApiCall { network.getLoginUrlInfo(loginType) }

    override suspend fun doLogin(
        code: String,
        state: String
    ): ApiOperation<LoginInfoResponse> = safeApiCall { network.doLogin(code, state, 0) }
}
