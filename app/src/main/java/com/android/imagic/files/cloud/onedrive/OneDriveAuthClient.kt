package com.android.imagic.files.cloud.onedrive

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

object OneDriveAuthClient {

    private val retrofitNetwork = Retrofit.Builder()
        .baseUrl("https://login.microsoftonline.com/")
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OneDriveAuthApi::class.java)

    /*val response = retrofitNetwork.refreshAccessToken(
        clientId = YOUR_CLIENT_ID,
        scope = "https://graph.microsoft.com/Files.ReadWrite.All offline_access",
        refreshToken = storedRefreshToken,
        clientSecret = YOUR_CLIENT_SECRET // 如果是 confidential client
    )*/
}