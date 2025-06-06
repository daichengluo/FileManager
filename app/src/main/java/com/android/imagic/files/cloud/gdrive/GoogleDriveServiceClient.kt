package com.android.imagic.files.cloud.gdrive

import com.android.imagic.files.app.networkJson
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.sdrive.client.TransferStatus
import com.android.imagic.files.cloud.sdrive.client.saveFile
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.guessFromPath
import com.android.imagic.files.filelist.name
import com.android.imagic.files.provider.common.createFile
import com.android.imagic.files.provider.common.exists
import com.android.imagic.files.provider.common.size
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.safeApiCall
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java8.nio.file.Path
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import java.io.File

class GoogleDriveServiceClient(val client: OkHttpClient) {

    private val json: Json = networkJson

    private val network = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(client)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(GoogleDriveService::class.java)

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

    suspend fun renameFile(fileId: String, newName: String): ApiOperation<String> =
        safeApiCall {
            val body = mapOf("name" to newName)
            network.updateFileName(fileId, body).id
        }

    suspend fun deleteFileById(fileId: String): ApiOperation<Unit> = safeApiCall {
        network.deleteFile(fileId)
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
}