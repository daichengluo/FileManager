package com.android.imagic.files.cloud.gdrive.api

import com.android.imagic.files.cloud.gdrive.FileOperationResponse
import com.android.imagic.files.cloud.gdrive.FileParentsResponse
import com.android.imagic.files.cloud.gdrive.GoogleDriveFileListResponse
import com.android.imagic.files.cloud.gdrive.RenameFileResponse
import com.android.imagic.files.cloud.gdrive.SimpleDriveFile
import com.android.imagic.files.cloud.gdrive.SimpleFileListResponse
import com.android.imagic.files.cloud.gdrive.StorageQuotaResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface GoogleDriveNetworkApi {

    // 获取文件列表（比如列出某个文件夹下的内容）
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Query("q") query: String,
        @Query("fields") fields: String = "files(id,name,mimeType,thumbnailLink,parents,size,createdTime,modifiedTime)",
        @Query("pageSize") pageSize: Int = 100
    ): GoogleDriveFileListResponse

    // 下载文件
    @GET("drive/v3/files/{fileId}")
    @Streaming
    suspend fun downloadFile(
        @Path("fileId") fileId: String,
        @Query("alt") alt: String = "media"
    ): ResponseBody

    // 下载文件
    @GET("drive/v3/files/{fileId}")
    @Streaming
    suspend fun downloadFile(
        @Path("fileId") fileId: String,
        @Header("Range") range: String,
        @Query("alt") alt: String = "media"
    ): ResponseBody

    // 上传文件 (multipart)
    @Multipart
    @POST("upload/drive/v3/files?uploadType=multipart")
    suspend fun uploadFile(
        @Part("metadata") metadata: RequestBody,
        @Part file: MultipartBody.Part
    ): FileOperationResponse

    @POST("upload/drive/v3/files?uploadType=resumable")
    suspend fun initiateResumableUpload(
        @Header("X-Upload-Content-Type") mimeType: String,
        @Body metadata: CreateFileRequest
    ): Response<Unit> // Location header 里有上传 URL

    @PUT
    suspend fun uploadFileChunk(
        @Url uploadUrl: String,
        @Header("Content-Length") length: Long,
        @Header("Content-Range") contentRange: String,
        @Body fileChunk: RequestBody
    ): Response<Unit>

    // 创建空文件夹
    @POST("drive/v3/files")
    suspend fun createDirectory(
        @Body body: CreateFileRequest
    ): FileOperationResponse

    @PATCH("drive/v3/files/{fileId}")
    suspend fun updateFileName(
        @Path("fileId") fileId: String,
        @Body body: Map<String, String>,
        @Query("fields") fields: String = "id, name"
    ): RenameFileResponse

    @GET("drive/v3/files/{fileId}")
    suspend fun getFileInfo(
        @Path("fileId") fileId: String,
        @Query("fields") fields: String = "id,mimeType"
    ): SimpleDriveFile

    @GET("drive/v3/files")
    suspend fun listChildren(
        @Query("q") query: String,
        @Query("fields") fields: String = "files(id)",
        @Query("pageSize") pageSize: Int = 1
    ): SimpleFileListResponse

    @DELETE("drive/v3/files/{fileId}")
    suspend fun deleteFile(
        @Path("fileId") fileId: String
    )

    @POST("drive/v3/files/{fileId}/copy")
    suspend fun copyFile(
        @Path("fileId") fileId: String,
        @Body body: CopyFileRequest
    ): FileOperationResponse

    @PATCH("drive/v3/files/{fileId}")
    suspend fun moveFile(
        @Path("fileId") fileId: String,
        @Query("addParents") newParentId: String,
        @Query("removeParents") oldParentId: String
    ): FileOperationResponse

    @GET("drive/v3/files/{fileId}")
    suspend fun getFileParents(
        @Path("fileId") fileId: String,
        @Query("fields") fields: String = "id,parents"
    ): FileParentsResponse

    @GET("drive/v3/about")
    suspend fun getStorageQuota(
        @Query("fields") fields: String = "storageQuota"
    ): StorageQuotaResponse
}
