package com.android.imagic.files.storage

import retrofit2.http.GET
import retrofit2.http.Query

internal interface SDriveAuthApi {
    @GET("Google/GetWhiteboardWeChatLoginUri")
    suspend fun getLoginUrlInfo(@Query("type") loginType: Int): LoginUrlResponse

    @GET("Google/GetWhiteboardWeChatLoginInfo")
    suspend fun doLogin(
        @Query("code") code: String,
        @Query("state") state: String,
        @Query("Automaticid") automaticId: Int,
    ): LoginInfoResponse
}