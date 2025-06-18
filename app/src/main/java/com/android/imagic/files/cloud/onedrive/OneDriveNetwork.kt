package com.android.imagic.files.cloud.onedrive

import android.util.Log
import com.android.imagic.files.app.networkJson
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.onedrive.UploadFileRequestItem.UploadFileRequest
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException
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
                UploadFileRequest(filename, "rename")
            )
            network.createUploadSession(parentId, filename, body).uploadUrl
        }

    fun uploadFileInChunks(
        uploadUrl: String,
        path: Path,
        chunkSize: Long = 256 * 1024L
    ): Flow<TransferStatus> = flow {
        val fileSize = path.size()
        val totalChunks = ((fileSize + chunkSize - 1) / chunkSize).toInt()
        Files.newByteChannel(path).use { channel ->
            repeat(totalChunks) { chunkIndex ->
                coroutineContext.ensureActive() // ✅ 每次循环检查是否被取消

                try {
                    val offset = chunkIndex * chunkSize
                    val bufferSize = min(chunkSize, fileSize - offset).toInt()
                    val byteArray = ByteArray(bufferSize)
                    val buffer = ByteBuffer.wrap(byteArray)

                    // 确保完整读取
                    var totalRead = 0
                    while (buffer.hasRemaining()) {
                        coroutineContext.ensureActive() // ✅ 读数据过程中检查
                        val read = channel.read(buffer)
                        if (read < 0) break
                        totalRead += read
                    }

                    if (totalRead <= 0) return@repeat

                    val start = offset
                    val end = offset + totalRead - 1

                    val request = Request.Builder()
                        .url(uploadUrl)
                        .addHeader("Content-Length", totalRead.toString())
                        .addHeader("Content-Range", "bytes $start-$end/$fileSize")
                        .put(
                            RequestBody.create(
                                "application/octet-stream".toMediaTypeOrNull(),
                                byteArray,
                                0,
                                totalRead
                            )
                        )
                        .build()

                    val response = client.newCall(request).execute()
                    response.use {
                        when (it.code) {
                            200, 201 -> {
                                emit(TransferStatus.Finished())
                                return@flow
                            }

                            202, 308 -> {
                                emit(TransferStatus.Transferring(end + 1, fileSize))
                                // 继续下一个 chunk
                            }

                            else -> {
                                emit(TransferStatus.Failed(Exception("Upload failed: ${it.code}")))
                                return@flow
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    // ✅ 明确处理协程取消
                    throw e
                } catch (e: Exception) {
                    emit(TransferStatus.Failed(e))
                    return@flow
                }
            }
        }
    }

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