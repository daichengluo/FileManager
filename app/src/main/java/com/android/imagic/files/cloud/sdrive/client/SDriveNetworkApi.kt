package com.android.imagic.files.cloud.sdrive.client

import com.android.imagic.files.util.ApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface SDriveNetworkApi {

    @HEAD
    suspend fun getContentLength(@Url url: String): Response<Void>

    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String, @Header("Range") range: String): ResponseBody

    @GET("api/NetDisk/GetUserFileList")
    suspend fun getFileList(@Query("TargetID") directoryId: Int): ApiResponse<List<FileInfo>>

    @Multipart
    @POST("api/OSSService/UploadFileAndGetUrl")
    suspend fun uploadFile(
        @Query("GroupID") userId: String,
        @Part file: MultipartBody.Part
    ): ApiResponse<UploadFileInfo>

    @Multipart
    @POST("api/OSSService/UploadChunkOss/uploadChunk")
    suspend fun uploadChunks(
        @Part("id") userId: RequestBody,
        @Part("chunkIndex") chunkIndex: RequestBody,
        @Part("totalChunks") totalChunks: RequestBody,
        @Part("fileName") fileName: RequestBody,
        @Part file: MultipartBody.Part
    ): ApiResponse<UploadFileInfo>

    @POST("api/NetDisk/AddNewUserFolder")
    suspend fun createDirectory(@Body body: NewFolderBody): ApiResponse<FileInfo>

    @POST("api/NetDisk/UploadUserFile")
    suspend fun uploadFileInfo(@Body body: FileInfoBody): ApiResponse<FileInfo>

    @PUT("api/NetDisk/ReNameUserFolderOrFile")
    suspend fun renameFile(@Body body: RenameBody): ApiResponse<Boolean>

    @POST("api/NetDisk/GetUserDirTree")
    suspend fun getDirTree(@Body body: DirTreeBody): ApiResponse<List<FileInfo>>

    @PUT("api/NetDisk/DeleteInformation")
    suspend fun deleteFiles(@Body body: DeleteFilesBody): ApiResponse<Int>

    @GET("api/NetDisk/UserPasteFolderOrFile")
    suspend fun moveOrCopyFiles(
        @Query("ArrayStr") ids: String,
        @Query("Type") type: Int,
        @Query("TargetID") directoryId: Int
    ): ApiResponse<Boolean>
}
