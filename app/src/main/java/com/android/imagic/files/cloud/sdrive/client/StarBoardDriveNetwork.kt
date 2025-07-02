package com.android.imagic.files.cloud.sdrive.client

import com.android.imagic.files.BuildConfig
import com.android.imagic.files.app.networkJson
import com.android.imagic.files.appwidget.AccessTokenData
import com.android.imagic.files.appwidget.toSpaceData
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.util.SpaceData
import com.android.imagic.files.cloud.util.TransferStatus
import com.android.imagic.files.cloud.util.saveFile
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.guessFromPath
import com.android.imagic.files.provider.common.createFile
import com.android.imagic.files.provider.common.size
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.safeApiCall
import com.android.imagic.files.util.safeApiCallHandled
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java8.nio.file.Files
import java8.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.math.min
import kotlin.math.pow

class StarBoardDriveNetwork(val client: OkHttpClient) : StarBoardDriveDataSource {

    private val baseUrl: String = BuildConfig.STB_API_URL
    private val json: Json = networkJson

    private val network = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(StarBoardDriveNetworkApi::class.java)

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
        val downloadedBytes = path.size()

        return network.downloadFile(
            url, "bytes=$downloadedBytes-"
        ).saveFile(path, downloadedBytes, totalBytes)
    }

    override suspend fun getFileList(directoryId: String): ApiOperation<List<CloudFileItem>> {
        return safeApiCallHandled {
            network.getFileList(directoryId)
        }.mapSuccess { list ->
            list.map { it.toCloudFileItem() }
        }
    }

    override suspend fun createDirectory(
        name: String,
        parentId: String
    ): ApiOperation<CloudFileItem> = safeApiCallHandled {
        val body = NewFolderBody(name, parentId)
        network.createDirectory(body)
    }.mapSuccess { it.toCloudFileItem() }

    override suspend fun uploadFile(userId: Int, file: File): ApiOperation<UploadFileInfo> =
        safeApiCallHandled {
            if (!file.exists()) file.createNewFile()
            val mimeType = MimeType.guessFromPath(file.path).value
            val requestBody = file.asRequestBody(mimeType.toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)
            network.uploadFile(userId.toString(), filePart)
        }

    override suspend fun uploadFile(userId: Int, path: Path): ApiOperation<UploadFileInfo> =
        safeApiCallHandled {
            if (!Files.exists(path)) path.createFile()
            val file = path.toFile()
            val mimeType = MimeType.guessFromPath(file.path).value
            val requestBody = file.asRequestBody(mimeType.toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)
            network.uploadFile(userId.toString(), filePart)
        }

    override suspend fun uploadChunks(
        userId: Int,
        path: Path,
        chunkSize: Long
    ): Flow<TransferStatus> = flow {
        val fileSize = path.size()
        val totalChunks = ((fileSize + chunkSize - 1) / chunkSize).toInt()
        val fileName = path.fileName.toString()

        Files.newByteChannel(path).use { channel ->
            repeat(totalChunks) { chunkIndex ->
                coroutineContext.ensureActive() // ✅ 每次循环检查是否被取消

                try {
                    val offset = chunkIndex * chunkSize
                    val bufferSize = min(chunkSize, fileSize - offset).toInt()

                    val byteArray = ByteArray(bufferSize)
                    val buffer = ByteBuffer.wrap(byteArray)
                    var totalRead = 0
                    while (buffer.hasRemaining()) {
                        coroutineContext.ensureActive() // ✅ 每次循环检查是否被取消
                        val read = channel.read(buffer)
                        if (read < 0) break
                        totalRead += read
                    }

                    val response = uploadChunkWithRetry(
                        userId = userId,
                        chunkIndex = chunkIndex,
                        totalChunks = totalChunks,
                        fileName = fileName,
                        byteArray = byteArray,
                    )

                    when (response) {
                        is ApiOperation.Failure -> {
                            emit(TransferStatus.Failed(response.exception))
                            return@flow
                        }

                        is ApiOperation.Success -> {
                            val url = response.data.url
                            if (url != null) {
                                emit(TransferStatus.Finished(url))
                                return@flow
                            } else {
                                emit(
                                    TransferStatus.Transferring(
                                        chunkSize * (chunkIndex + 1),
                                        fileSize
                                    )
                                )
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

    private suspend fun uploadChunkWithRetry(
        userId: Int,
        chunkIndex: Int,
        totalChunks: Int,
        fileName: String,
        byteArray: ByteArray,
        maxRetry: Int = 10,
        baseDelayMillis: Long = 1000L
    ): ApiOperation<UploadFileInfo> = withContext(Dispatchers.IO) {
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
                return@withContext response
            }
            if (attempt < maxRetry) {
                val delayTime = baseDelayMillis * (2.0.pow(attempt - 1)).toLong()
                    .coerceAtMost(60_000L)
                delay(delayTime)
            }
        }

        return@withContext ApiOperation.Failure(
            IOException("The upload failed after retrying $maxRetry several times")
        )
    }

    override suspend fun uploadFileInfo(
        name: String,
        size: Long,
        url: String,
        targetId: String
    ): ApiOperation<Boolean> = safeApiCallHandled {
        val body = FileInfoBody(name, size, url, "", targetId)
        network.uploadFileInfo(body)
    }.mapSuccess { true }

    override suspend fun renameFile(newName: String, fileId: String): ApiOperation<Boolean> {
        return safeApiCallHandled {
            val body = RenameBody(fileId, newName)
            network.renameFile(body)
        }
    }

    override suspend fun getDirTree(directoryId: String): ApiOperation<List<CloudFileItem>> {
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

    override suspend fun moveFiles(fileIds: String, targetId: String): ApiOperation<Boolean> {
        return safeApiCallHandled {
            network.moveOrCopyFiles(fileIds, 2, targetId)
        }
    }

    override suspend fun copyFiles(fileIds: String, targetId: String): ApiOperation<Boolean> {
        return safeApiCallHandled {
            network.moveOrCopyFiles(fileIds, 1, targetId)
        }
    }

    suspend fun getSpaceData(): ApiOperation<SpaceData> = safeApiCallHandled {
        network.getSpaceUsageInfo()
    }.mapSuccess { it.toSpaceData() }

    override suspend fun refreshAccessToken(
        type: Int,
        refreshToken: String
    ): ApiOperation<AccessTokenData> = safeApiCallHandled {
        network.refreshAccessToken(type, refreshToken)
    }
}
