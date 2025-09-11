package com.android.imagic.files.cloud.gdrive.api

import com.android.imagic.files.app.networkJson
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.gdrive.toCloudFileItem
import com.android.imagic.files.cloud.gdrive.toSpaceData
import com.android.imagic.files.cloud.util.SpaceData
import com.android.imagic.files.cloud.util.TransferStatus
import com.android.imagic.files.cloud.util.saveFile
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.guessFromPath
import com.android.imagic.files.filelist.name
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
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.math.min

class GoogleDriveNetwork(val client: OkHttpClient) {

    private val json: Json = networkJson

    private val network = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(client)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(GoogleDriveNetworkApi::class.java)

    suspend fun getFileList(
        directoryId: String,
        trashed: Boolean = false
    ): ApiOperation<List<CloudFileItem>> = safeApiCall {
        val query = "'$directoryId' in parents and trashed = $trashed"
        network.listFiles(query).files.map { it.toCloudFileItem() }
    }

    suspend fun createDirectory(directoryName: String, parentId: String): ApiOperation<String> =
        safeApiCall {
            val body = CreateFileRequest(
                name = directoryName,
                mimeType = "application/vnd.google-apps.folder",
                parents = listOf(parentId)
            )
            network.createDirectory(body).id
        }

    suspend fun uploadFile(file: File, parentId: String): ApiOperation<String> =
        safeApiCall {
            if (!file.exists()) file.createNewFile()
            val mimeType = MimeType.guessFromPath(file.path).value
            val body = CreateFileRequest(
                name = file.name,
                mimeType = mimeType,
                parents = listOf(parentId)
            )
            val metadata = Json.encodeToString(body)
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val requestBody = file.asRequestBody(mimeType.toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)
            network.uploadFile(metadata, filePart).id
        }

    suspend fun uploadFile(path: Path, parentId: String): ApiOperation<String> =
        safeApiCall {
            if (!path.exists()) path.createFile()
            val file = path.toFile()
            val mimeType = MimeType.guessFromPath(file.path).value
            val body = CreateFileRequest(
                name = file.name,
                mimeType = mimeType,
                parents = listOf(parentId)
            )
            val metadata = Json.encodeToString(body)
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val requestBody = file.asRequestBody(mimeType.toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)
            network.uploadFile(metadata, filePart).id
        }

    suspend fun getResumableUploadUrl(path: Path, parentId: String): ApiOperation<String?> =
        safeApiCall {
            val mimeType = MimeType.guessFromPath(path.toString()).value
            val body = CreateFileRequest(
                name = path.name,
                mimeType = mimeType,
                parents = listOf(parentId)
            )
            network.initiateResumableUpload(mimeType, body).headers()["Location"]
        }

    fun uploadFileInChunks(
        uploadUrl: String,
        path: Path,
        chunkSize: Long = 1024 * 1024L
    ): Flow<TransferStatus> = flow {
        val fileSize = path.size()
        val totalChunks = ((fileSize + chunkSize - 1) / chunkSize).toInt()
        Files.newByteChannel(path).use { channel ->
            coroutineContext.ensureActive()

            repeat(totalChunks) { chunkIndex ->
                try {
                    val offset = chunkIndex * chunkSize
                    val bufferSize = min(chunkSize, fileSize - offset).toInt()
                    val byteArray = ByteArray(bufferSize)
                    val buffer = ByteBuffer.wrap(byteArray)

                    var totalRead = 0
                    while (buffer.hasRemaining()) {
                        coroutineContext.ensureActive()
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
                        .put(RequestBody.create(null, byteArray.copyOf(totalRead)))
                        .build()

                    val response = client.newCall(request).execute()
                    response.use {
                        when {
                            it.isSuccessful -> {
                                emit(TransferStatus.Finished())
                            }

                            it.code == 308 -> { // Resume Incomplete
                                emit(
                                    TransferStatus.Transferring(end + 1, fileSize)
                                )
                            }

                            else -> {
                                emit(TransferStatus.Failed(Exception("Upload failed: ${it.code}")))
                            }
                        }
                    }

                } catch (e: CancellationException) {
                    emit(TransferStatus.Failed(e))
                } catch (e: Exception) {
                    emit(TransferStatus.Failed(e))
                }
            }
        }
    }

    suspend fun renameFile(fileId: String, newName: String): ApiOperation<String> =
        safeApiCall {
            val body = mapOf("name" to newName)
            network.updateFileName(fileId, body).id
        }

    suspend fun deleteFileById(fileId: String): ApiOperation<Unit> = safeApiCall {
        network.deleteFile(fileId)
    }

    suspend fun deleteEmptyFolder(fileId: String): ApiOperation<Unit> = safeApiCall {
        val file = network.getFileInfo(fileId)
        if (file.mimeType == "application/vnd.google-apps.folder") {
            val children = network.listChildren("'$fileId' in parents and trashed=false")
            if (children.files.isEmpty()) {
                network.deleteFile(fileId)
            }
        }
    }

    suspend fun deleteEmptyFoldersRecursive(folderId: String): ApiOperation<Unit> = safeApiCall {
        val file = network.getFileInfo(folderId)
        if (file.mimeType != "application/vnd.google-apps.folder") {
            return@safeApiCall
        }

        // 获取子文件
        val children = network.listChildren("'$folderId' in parents and trashed=false").files

        if (children.isEmpty()) {
            // 当前就是空文件夹 → 删除
            network.deleteFile(folderId)
        }

        for (child in children) {
            if (child.mimeType == "application/vnd.google-apps.folder") {
                deleteEmptyFoldersRecursive(child.id)
            }
        }

        // 再次检查是否变空
        val recheck = network.listChildren("'$folderId' in parents and trashed=false").files
        if (recheck.isEmpty()) {
            network.deleteFile(folderId)
        }
    }

    suspend fun downloadFile(fileId: String, fileSize: Long, path: Path): Flow<TransferStatus> {
        val downloadedBytes = path.size()
        return network.downloadFile(fileId).saveFile(path, downloadedBytes, fileSize)
    }

    suspend fun copyFile(fileId: String, targetFileId: String): ApiOperation<String> = safeApiCall {
        val body = CopyFileRequest(
            parents = listOf(targetFileId)
        )
        network.copyFile(fileId, body).id
    }

    suspend fun moveFile(
        fileId: String,
        targetFileId: String,
        sourceFileId: String
    ): ApiOperation<String> = safeApiCall {
        network.moveFile(fileId, targetFileId, sourceFileId).id
    }

    suspend fun getSpaceData(): ApiOperation<SpaceData> = safeApiCall {
        network.getStorageQuota().storageQuota.toSpaceData()
    }
}