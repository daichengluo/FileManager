package com.android.imagic.files.appwidget

import com.android.imagic.files.util.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

internal interface AuthApi {
    @GET("getState")
    suspend fun getState(@Query("state") state: String): ApiResponse<String>

    @GET("getAccessToken")
    suspend fun getAccessToken(@Query("state") state: String): ApiResponse<AccessTokenData>
}