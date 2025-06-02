package com.android.imagic.files.cloud.gdrive

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface GoogleDriveService {

    // 获取文件列表（比如列出某个文件夹下的内容）
    @GET("files")
    suspend fun listFiles(
        @Query("q") query: String,
        @Query("fields") fields: String = "files(id,name,mimeType,parents,size,createdTime,modifiedTime)",
        @Query("pageSize") pageSize: Int = 100
    ): GoogleDriveFileListResponse

    // 下载文件
    @GET("files/{fileId}")
    @Streaming
    suspend fun downloadFile(
        @Path("fileId") fileId: String,
        @Query("alt") alt: String = "media"
    ): Response<ResponseBody>

    // 上传文件 (multipart)
    @Multipart
    @POST("upload/drive/v3/files?uploadType=multipart")
    suspend fun uploadFile(
        @Part("metadata") metadata: RequestBody,
        @Part file: MultipartBody.Part
    ): GoogleDriveFileMetadata

    // 创建空文件夹
    @POST("files")
    suspend fun createFolder(
        @Body body: CreateFolderRequest
    ): CreateFolderResponse
}
