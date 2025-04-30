package com.android.imagic.files.provider.cloud.sdrive.client

import android.util.Log
import com.android.imagic.files.BuildConfig
import com.android.imagic.files.app.networkJson
import com.android.imagic.files.file.cloud.sdrive.CloudFileItem
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.safeApiCall
import com.android.imagic.files.util.safeApiCallHandled
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java8.nio.file.Files
import java8.nio.file.Path
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import java.io.File

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
        Log.d(
            "chengluo",
            "downloadFile url: $url path: $path downloadedBytes: $downloadedBytes totalBytes: $totalBytes"
        )

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
            val body = file.toMultipartBody()
            network.uploadFile(userId.toString(), body)
        }
    }

    override suspend fun uploadFileInfo(body: FileInfoBody): ApiOperation<Boolean> {
        return safeApiCallHandled { network.uploadFileInfo(body) }.mapSuccess { true }
    }

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

fun File.toMultipartBody(): MultipartBody.Part = MultipartBody.Part.createFormData(
    "file",
    name,
    asRequestBody("application/octet-stream".toMediaType())
)