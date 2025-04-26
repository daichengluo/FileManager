package com.android.imagic.files.provider.cloud.sdrive.client

import com.android.imagic.files.BuildConfig
import com.android.imagic.files.app.networkJson
import com.android.imagic.files.file.cloud.sdrive.CloudFileItem
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.safeApiCall
import com.android.imagic.files.util.safeApiCallHandled
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
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
            val headers = network.getContentLength(url).headers()
            headers["Content-Length"]?.toLong() ?: 0
        }
    }

    override suspend fun downloadFile(url: String, file: File): Flow<TransferStatus> {
        val downloadedBytes = if (file.exists()) file.length() else 0L
        return network.downloadFile(
            url, "bytes=$downloadedBytes-"
        ).saveFile(file, downloadedBytes)
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

    override suspend fun uploadFile(userId: Int, file: File) {

    }

    override suspend fun uploadFileInfo(body: FileInfoBody): ApiOperation<Boolean> =
        safeApiCallHandled { network.uploadFileInfo(body) }

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