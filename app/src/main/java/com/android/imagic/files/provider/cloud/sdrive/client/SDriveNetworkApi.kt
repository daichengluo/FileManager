package com.android.imagic.files.provider.cloud.sdrive.client

import com.android.imagic.files.util.ApiResponse
import java8.nio.file.Path
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
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
        @Part("GroupID") userId: Int,
        @Part file: MultipartBody.Part
    ): ApiResponse<UploadFileInfo>

    @POST("api/NetDisk/AddNewUserFolder")
    suspend fun createDirectory(@Body body: NewFolderBody): ApiResponse<FileInfo>

    @POST("api/NetDisk/UploadUserFile")
    suspend fun uploadFileInfo(@Body body: FileInfoBody): ApiResponse<Boolean>

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

fun String.toRequestBody(mediaType: MediaType = "text/plain".toMediaType()): RequestBody =
    RequestBody.create(mediaType, this)

fun Path.toMultipartBody(): MultipartBody.Part {
    val file = this.toFile()
    val requestFile = file.asRequestBody("application/octet-stream".toMediaType())
    return MultipartBody.Part.createFormData("file", file.name, requestFile)
}