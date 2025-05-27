package com.android.imagic.files.appwidget

import com.android.imagic.files.util.ApiResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

internal interface CloudApi {
    @GET("Google/GetWhiteboardWeChatLoginUri")
    suspend fun getLoginUrlInfo(@Query("type") loginType: Int): ApiResponse<LoginUrl>

    @GET("Google/GetWhiteboardWeChatLoginInfo")
    suspend fun doLogin(
        @Query("code") code: String,
        @Query("state") state: String,
        @Query("Automaticid") automaticId: Int,
    ): ApiResponse<LoginInfo>

    @POST("api/ChatGroup/GetUserInformation")
    suspend fun getUserInfo(@Body body: AuthInfoBody): ApiResponse<LoginInfo>

    @Multipart
    @POST("api/FileUploadServices/UploadFile")
    suspend fun uploadFile(@Part file: MultipartBody.Part): ApiResponse<List<String>>

    @GET("api/NetDisk/Getstoragespace")
    suspend fun getSpaceUsageInfo(): ApiResponse<SpaceInfo>
}