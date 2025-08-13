package com.android.imagic.files.cloud.onedrive

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface OneDriveNetworkApi {

    @GET("me/drive/root/children")
    suspend fun listRootFiles(): OneDriveListResponse

    @GET("me/drive/items/{itemId}/children")
    suspend fun listDirectoryFiles(@Path("itemId") itemId: String): OneDriveListResponse

    @POST("me/drive/root/children")
    suspend fun createFolderInRoot(
        @Body folderRequest: CreateFileRequest
    ): OneDriveFileMetadata

    @POST("me/drive/items/{parentId}/children")
    suspend fun createFolder(
        @Path("parentId") parentId: String,
        @Body folderRequest: CreateFileRequest
    ): OneDriveFileMetadata

    @PUT("me/drive/root:/{pathAndFilename}:/content")
    suspend fun uploadFileInRoot(
        @Path("pathAndFilename", encoded = true) path: String,
        @Body fileBody: RequestBody
    ): OneDriveFileMetadata

    @PUT("me/drive/items/{parentId}:/{filename}:/content")
    suspend fun uploadFile(
        @Path("parentId") parentId: String,
        @Path("filename", encoded = true) filename: String,
        @Body fileBody: RequestBody
    ): OneDriveFileMetadata

    @POST("me/drive/root:/{filename}:/createUploadSession")
    suspend fun createUploadSessionInRoot(
        @Path("filename") filename: String,
        @Body uploadFileRequestItem: UploadFileRequestItem
    ): UploadSessionResponse

    @POST("me/drive/items/{parentId}:/{filename}:/createUploadSession")
    suspend fun createUploadSession(
        @Path("parentId") parentId: String,
        @Path("filename") filename: String,      // 仅文件名，不含斜杠
        @Body uploadFileRequestItem: UploadFileRequestItem
    ): UploadSessionResponse

    @DELETE("me/drive/items/{itemId}")
    suspend fun deleteItemById(
        @Path("itemId") itemId: String
    ): Response<Unit>

    @PATCH("me/drive/items/{itemId}")
    suspend fun renameItemById(
        @Path("itemId") itemId: String,
        @Body body: Map<String, String>
    ): OneDriveFileMetadata

    @POST("me/drive/items/{itemId}/copy")
    suspend fun copyItem(
        @Path("itemId") itemId: String,
        @Body operationFileRequest: OperationFileRequest
    ): Response<Unit>  // 返回 202，无实体

    @PATCH("me/drive/items/{itemId}")
    suspend fun moveItem(
        @Path("itemId") itemId: String,
        @Body operationFileRequest: OperationFileRequest
    ): OneDriveFileMetadata

    @GET("me/drive/items/{itemId}")
    suspend fun getDownloadUrl(
        @Path("itemId") itemId: String,
        @Query("\$select") select: String = "@microsoft.graph.downloadUrl"
    ): OneDriveDownloadResponse

    @GET("me/drive")
    suspend fun getStorageQuota(): StorageQuotaResponse

    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String, @Header("Range") range: String): ResponseBody
}