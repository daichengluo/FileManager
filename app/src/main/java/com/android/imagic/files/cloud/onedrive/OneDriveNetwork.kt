package com.android.imagic.files.cloud.onedrive

import android.util.Log
import com.android.imagic.files.app.networkJson
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.util.ProgressRequestBody
import com.android.imagic.files.cloud.util.SpaceData
import com.android.imagic.files.cloud.util.TransferStatus
import com.android.imagic.files.cloud.util.saveFile
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.guessFromPath
import com.android.imagic.files.provider.common.createFile
import com.android.imagic.files.provider.common.exists
import com.android.imagic.files.provider.common.size
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.safeApiCall
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java8.nio.file.Files
import java8.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.math.min

class OneDriveNetwork(val client: OkHttpClient) {

    private val json: Json = networkJson

    private val network = Retrofit.Builder()
        .baseUrl("https://graph.microsoft.com/v1.0/")
        .client(client)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(OneDriveNetworkApi::class.java)

    fun getNewUploadApi(): OneDriveUploadChunkApi {
        val client = OkHttpClient().newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
            )
            .build()
        return Retrofit.Builder()
            .baseUrl("https://graph.microsoft.com/v1.0/")
            .client(client)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(OneDriveUploadChunkApi::class.java)
    }

    suspend fun getRootFileList(): ApiOperation<List<CloudFileItem>> = safeApiCall {
        network.listRootFiles().value.map { it.toCloudFileItem() }
    }

    suspend fun getFileList(directoryId: String): ApiOperation<List<CloudFileItem>> = safeApiCall {
        network.listDirectoryFiles(directoryId).value.map { it.toCloudFileItem() }
    }

    suspend fun createDirectory(name: String, parentId: String): ApiOperation<CloudFileItem> =
        safeApiCall {
            val createFolderRequest = CreateFileRequest(name, emptyMap(), "rename")
            network.createFolder(parentId, createFolderRequest).toCloudFileItem()
        }

    suspend fun getUploadUrl(parentId: String, filename: String): ApiOperation<String?> =
        safeApiCall {
            val body = UploadFileRequestItem(
                UploadFileRequestItem.UploadFileRequest("rename")
            )
            network.createUploadSession(parentId, filename, body).uploadUrl
        }

    fun uploadFileInChunks(
        api: OneDriveUploadChunkApi,
        uploadUrl: String,
        file: Path,
        chunkSize: Long = 1024 * 1024L // 1MB
    ): Flow<TransferStatus> = flow {
        val fileSize = file.size()
        val totalChunks = ((fileSize + chunkSize - 1) / chunkSize).toInt()

        Files.newByteChannel(file).use { channel ->
            repeat(totalChunks) { chunkIndex ->
                coroutineContext.ensureActive()

                val offset = chunkIndex * chunkSize
                val bufferSize = min(chunkSize, fileSize - offset).toInt()
                val byteArray = ByteArray(bufferSize)
                val buffer = ByteBuffer.wrap(byteArray)
                var totalRead = 0
                while (buffer.hasRemaining()) {
                    val read = channel.read(buffer)
                    if (read < 0) break
                    totalRead += read
                }
                if (totalRead <= 0) return@repeat

                val start = offset
                val end = offset + totalRead - 1
                val requestBody = byteArray.toRequestBody(
                    MimeType.guessFromPath(file.toString()).value.toMediaType(),
                    0,
                    totalRead
                )

                val response = api.uploadChunk(
                    uploadUrl = uploadUrl,
                    contentLength = totalRead.toLong(),
                    contentRange = "bytes $start-$end/$fileSize",
                    body = requestBody
                )

                when (response.code()) {
                    200, 201 -> {
                        emit(TransferStatus.Finished())
                    }

                    202, 308 -> {
                        emit(TransferStatus.Transferring(end + 1, fileSize))
                    }

                    else -> {
                        emit(TransferStatus.Failed(Exception("Upload failed: ${response.code()}")))
                    }
                }
            }
        }
    }

    fun uploadFileWithProgress(
        file: File,
        parentId: String
    ): Flow<TransferStatus> = callbackFlow {
        if (!file.exists()) file.createNewFile()

        val mimeType = MimeType.guessFromPath(file.path).value
        val mediaType = mimeType.toMediaType()

        val requestBody = ProgressRequestBody(file, mediaType) { transferred, total ->
            trySend(TransferStatus.Transferring(transferred, total))
        }

        try {
            val encodedName = URLEncoder.encode(file.name, "UTF-8")
            val metadata = network.uploadFile(parentId, encodedName, requestBody)
            trySend(TransferStatus.Finished(data = metadata.id))
        } catch (e: Exception) {
            trySend(TransferStatus.Failed(e))
        }

        close() // 结束 flow
    }.flowOn(Dispatchers.IO)

    suspend fun uploadFile(file: File, parentId: String): ApiOperation<CloudFileItem> =
        safeApiCall {
            if (!file.exists()) file.createNewFile()
            val mimeType = MimeType.guessFromPath(file.path).value
            val requestBody = file.asRequestBody(mimeType.toMediaType())
            network.uploadFile(parentId, file.name, requestBody).toCloudFileItem()
        }

    suspend fun uploadFile(path: Path, parentId: String): ApiOperation<CloudFileItem> =
        safeApiCall {
            if (!path.exists()) path.createFile()
            val file = path.toFile()
            val mimeType = MimeType.guessFromPath(file.path).value
            val requestBody = file.asRequestBody(mimeType.toMediaType())
            network.uploadFile(parentId, file.name, requestBody).toCloudFileItem()
        }

    suspend fun getDownloadUrl(fileId: String): ApiOperation<String> =
        safeApiCall { network.getDownloadUrl(fileId).downloadUrl }

    suspend fun downloadFile(url: String, fileSize: Long, path: Path): Flow<TransferStatus> {
        Log.d("chengluo", "downloadFile url: $url")
        val downloadedBytes = path.size()
        return network.downloadFile(url, "bytes=$downloadedBytes-")
            .saveFile(path, downloadedBytes, fileSize)
    }

    suspend fun renameFile(fileId: String, newName: String): ApiOperation<CloudFileItem> =
        safeApiCall {
            val body = mapOf("name" to newName)
            network.renameItemById(fileId, body).toCloudFileItem()
        }

    suspend fun deleteFileById(fileId: String): ApiOperation<Unit> = safeApiCall {
        network.deleteItemById(fileId)
    }

    suspend fun copyFile(fileId: String, targetFileId: String): ApiOperation<Unit> = safeApiCall {
        val body = OperationFileRequest(ParentReference(targetFileId))
        network.copyItem(fileId, body)
    }

    suspend fun moveFile(fileId: String, targetFileId: String): ApiOperation<CloudFileItem> =
        safeApiCall {
            val body = OperationFileRequest(ParentReference(targetFileId))
            network.moveItem(fileId, body).toCloudFileItem()
        }

    suspend fun getSpaceData(): ApiOperation<SpaceData> = safeApiCall {
        network.getStorageQuota().quota.toSpaceData()
    }
}