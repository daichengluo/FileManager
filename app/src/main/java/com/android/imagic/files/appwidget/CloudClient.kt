package com.android.imagic.files.appwidget

import com.android.imagic.files.BuildConfig
import com.android.imagic.files.app.networkJson
import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.safeApiCallHandled
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import java.io.File

internal interface CloudAuth {
    suspend fun getLoginUrlInfo(loginType: Int): ApiOperation<LoginUrl>
    suspend fun doLogin(code: String, state: String): ApiOperation<LoginInfo>
    suspend fun getUserInfo(body: AuthInfoBody): ApiOperation<LoginInfo>
    suspend fun uploadFileService(file: File): ApiOperation<String?>
    suspend fun getSpaceUsageInfo(): ApiOperation<SpaceInfo>
    suspend fun refreshAccessToken(type: Int, refreshToken: String): ApiOperation<ServerAuthority>
}

@Serializable
data class LoginUrl(
    val loginURL: String,
    val stateCode: String
)

@Serializable
data class AuthInfoBody(
    val login_type: String,
    val email: String,
    val unionid: String,
    val refresh_token: String,
    val nickname: String,
    val headimgurl: String
)

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
) {
    val isGoogle: Boolean
        get() = login_type == "1"

    val isMicrosoft: Boolean
        get() = login_type == "2"

    val isWeChat: Boolean
        get() = login_type == "3"
}

@Serializable
data class SpaceInfo(
    val remainingSpace: Long,
    val usedSpace: Long,
    val totalSpace: Long
)

fun LoginInfo.toAuthority(): Authority = Authority(
    openId = openid,
    accessToken = access_token,
    userId = id,
    username = nickname,
    headerImageUrl = headimgurl
)

class CloudClient(client: OkHttpClient) : CloudAuth {
    private val baseUrl = BuildConfig.STB_API_URL
    private val json = networkJson

    private val network = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(CloudApi::class.java)

    override suspend fun getLoginUrlInfo(loginType: Int): ApiOperation<LoginUrl> =
        safeApiCallHandled { network.getLoginUrlInfo(loginType) }

    override suspend fun doLogin(
        code: String,
        state: String
    ): ApiOperation<LoginInfo> = safeApiCallHandled { network.doLogin(code, state, 0) }

    override suspend fun getUserInfo(body: AuthInfoBody): ApiOperation<LoginInfo> =
        safeApiCallHandled { network.getUserInfo(body) }

    override suspend fun uploadFileService(file: File): ApiOperation<String?> {
        return safeApiCallHandled {
            val requestBody = file.asRequestBody("image/jpeg".toMediaType())

            val multipart = MultipartBody.Part.createFormData(
                name = "file",
                filename = file.name,
                body = requestBody
            )
            network.uploadFile(multipart)
        }.mapSuccess { it.firstOrNull() }
    }

    override suspend fun getSpaceUsageInfo(): ApiOperation<SpaceInfo> =
        safeApiCallHandled { network.getSpaceUsageInfo() }

    override suspend fun refreshAccessToken(
        type: Int,
        refreshToken: String
    ): ApiOperation<ServerAuthority> {
        return safeApiCallHandled {
            network.refreshAccessToken(type, refreshToken)
        }.mapSuccess { it.toServerAuthority() }
    }
}
