package com.android.imagic.files.provider.cloud.sdrive.client

import com.android.imagic.files.BuildConfig
import com.android.imagic.files.app.networkJson
import com.android.imagic.files.file.cloud.sdrive.CloudFileItem
import com.android.imagic.files.provider.common.createFile
import com.android.imagic.files.provider.common.size
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.safeApiCall
import com.android.imagic.files.util.safeApiCallHandled
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java8.nio.file.Files
import java8.nio.file.Path
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.min
import kotlin.math.pow

class SDriveNetworkClient(val client: OkHttpClient) : SDriveDataSource {

    private val baseUrl: String = BuildConfig.STB_API_URL
    private val json: Json = networkJson

    private val network = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(SDriveNetworkApi::class.java)

    override suspend fun getContentLength(url: String): ApiOperation<Long> {
        return safeApiCall {
            network.getContentLength(url).headers()["Content-Length"]?.toLong() ?: 0L
        }
    }

    override suspend fun downloadFile(url: String, path: Path): Flow<TransferStatus> {
        val response = getContentLength(url)
        val totalBytes = if (response is ApiOperation.Success) {
            response.data
        } else {
            0L
        }
        val downloadedBytes = Files.size(path)

        return network.downloadFile(
            url, "bytes=$downloadedBytes-"
        ).saveFile(path, downloadedBytes, totalBytes)
    }

    override suspend fun getFileList(directoryId: Int): ApiOperation<List<CloudFileItem>> {
        return safeApiCallHandled {
            network.getFileList(directoryId)
        }.mapSuccess { list ->
            list.map { it.toCloudFileItem() }
        }
    }

    override suspend fun createDirectory(name: String, parentId: Int): ApiOperation<Boolean> {
        return safeApiCallHandled {
            val body = NewFolderBody(name, parentId)
            network.createDirectory(body)
        }.mapSuccess { true }
    }

    override suspend fun uploadFile(userId: Int, file: File): ApiOperation<UploadFileInfo> {
        return safeApiCallHandled {
            if (!file.exists()) file.createNewFile()
            val requestBody = file.asRequestBody("application/octet-stream".toMediaType())
            val partBody = MultipartBody.Part.createFormData("file", file.name, requestBody)
            network.uploadFile(userId.toString(), partBody)
        }
    }

    override suspend fun uploadFile(userId: Int, path: Path): ApiOperation<UploadFileInfo> {
        return safeApiCallHandled {
            if (!Files.exists(path)) path.createFile()
            val file = path.toFile()
            val requestBody = file.asRequestBody("application/octet-stream".toMediaType())
            val partBody = MultipartBody.Part.createFormData("file", file.name, requestBody)
            network.uploadFile(userId.toString(), partBody)
        }
    }

    override suspend fun uploadChunks(userId: Int, path: Path): Flow<TransferStatus> =
        flow {
            val fileSize = path.size()
            val chunkSize = when {
                fileSize < 20 * 1024 * 1024 -> 1 * 1024 * 1024L  // <20MB，1MB
                fileSize < 100 * 1024 * 1024 -> 2 * 1024 * 1024L // <100MB，2MB
                else -> 4 * 1024 * 1024L                         // >100MB，4MB
            }
            val totalChunks = ((fileSize + chunkSize - 1) / chunkSize).toInt()
            val fileName = path.fileName.toString()

            Files.newByteChannel(path).use { channel ->
                for (chunkIndex in 0 until totalChunks) {
                    val bufferSize = min(chunkSize, fileSize - chunkIndex * chunkSize).toInt()
                    val buffer = ByteBuffer.allocate(bufferSize)
                    channel.read(buffer)
                    buffer.flip()

                    val byteArray = ByteArray(buffer.remaining())
                    buffer.get(byteArray)

                    val response = uploadChunkWithRetry(
                        userId = userId,
                        chunkIndex = chunkIndex,
                        totalChunks = totalChunks,
                        fileName = fileName,
                        byteArray = byteArray,
                    )
                    when (response) {
                        is ApiOperation.Failure -> emit(TransferStatus.Failed(response.exception))
                        is ApiOperation.Success -> {
                            val url = response.data.url
                            if (url == null) {
                                emit(
                                    TransferStatus.Transferring(
                                        chunkSize * (chunkIndex + 1),
                                        fileSize
                                    )
                                )
                            } else {
                                emit(TransferStatus.Finished(url))
                            }
                        }
                    }
                }
            }
        }

    private suspend fun uploadChunkWithRetry(
        userId: Int,
        chunkIndex: Int,
        totalChunks: Int,
        fileName: String,
        byteArray: ByteArray,
        maxRetry: Int = 10,
        baseDelayMillis: Long = 1000L // 初始延迟 0.5 秒
    ): ApiOperation<UploadFileInfo> {
        val requestBody = byteArray.toRequestBody("application/octet-stream".toMediaType())
        val partBody = MultipartBody.Part.createFormData("file", fileName, requestBody)

        val chunkIndexBody = chunkIndex.toString().toRequestBody("text/plain".toMediaType())
        val totalChunksBody = totalChunks.toString().toRequestBody("text/plain".toMediaType())
        val fileNameBody = fileName.toRequestBody("text/plain".toMediaType())
        val userIdBody = userId.toString().toRequestBody("text/plain".toMediaType())

        for (attempt in 1..maxRetry) {
            val response = safeApiCallHandled {
                network.uploadChunks(
                    userId = userIdBody,
                    chunkIndex = chunkIndexBody,
                    totalChunks = totalChunksBody,
                    fileName = fileNameBody,
                    file = partBody
                )
            }
            if (response is ApiOperation.Success) {
                return response
            }
            if (attempt < maxRetry) {
                val delayTime = baseDelayMillis * (2.0.pow(attempt - 1)).toLong()
                    .coerceAtMost(60_000L) // 最长延迟 30 秒
                delay(delayTime)
            }
        }

        return ApiOperation.Failure(IOException("The upload failed after retrying $maxRetry several times"))
    }

    override suspend fun uploadFileInfo(
        name: String,
        size: Int,
        url: String,
        directoryId: Int
    ): ApiOperation<Boolean> = safeApiCallHandled {
        val body = FileInfoBody(name, size, url, "", directoryId)
        network.uploadFileInfo(body)
    }.mapSuccess { true }

    override suspend fun renameFile(newName: String, fileId: Int): ApiOperation<Boolean> {
        return safeApiCallHandled {
            val body = RenameBody(fileId, newName)
            network.renameFile(body)
        }
    }

    override suspend fun getDirTree(directoryId: Int): ApiOperation<List<CloudFileItem>> {
        return safeApiCallHandled {
            val body = DirTreeBody(directoryId)
            network.getDirTree(body)
        }.mapSuccess { list ->
            list.map { it.toCloudFileItem() }
        }
    }

    override suspend fun deleteFiles(fileIds: String): ApiOperation<Boolean> {
        return safeApiCallHandled {
            val body = DeleteFilesBody(fileIds)
            network.deleteFiles(body)
        }.mapSuccess { it > 0 }
    }

    override suspend fun moveFiles(fileIds: String, targetId: Int): ApiOperation<Boolean> {
        return safeApiCallHandled {
            network.moveOrCopyFiles(fileIds, 2, targetId)
        }
    }

    override suspend fun copyFiles(fileIds: String, targetId: Int): ApiOperation<Boolean> {
        return safeApiCallHandled {
            network.moveOrCopyFiles(fileIds, 1, targetId)
        }
    }
}
