package com.android.imagic.files.cloud.onedrive

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OneDriveAuthApi {

    @FormUrlEncoded
    @POST("common/oauth2/v2.0/token")
    suspend fun refreshAccessToken(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("client_secret") clientSecret: String? = null // 若为 confidential client
    ): TokenResponse
}