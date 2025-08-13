package com.android.imagic.files.cloud.onedrive

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Url

interface OneDriveUploadChunkApi {
    @PUT
    suspend fun uploadChunk(
        @Url uploadUrl: String, // 直接用微软返回的 uploadUrl
        @Header("Content-Length") contentLength: Long,
        @Header("Content-Range") contentRange: String,
        @Body body: RequestBody
    ): Response<ResponseBody>
}