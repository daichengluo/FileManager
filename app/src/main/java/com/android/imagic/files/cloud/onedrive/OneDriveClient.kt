package com.android.imagic.files.cloud.onedrive

import android.util.Log
import com.android.imagic.files.app.okHttpClient
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.util.AuthenticatorInterceptor
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.cloud.util.TransferStatus
import com.android.imagic.files.provider.common.size
import com.android.imagic.files.util.ApiOperation
import java8.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

object OneDriveClient {

    private val clients = mutableMapOf<ServerAuthority, OneDriveNetwork>()

    private fun getClient(authority: ServerAuthority): OneDriveNetwork {
        synchronized(clients) {
            var client = clients[authority]
            if (client == null) {
                val authenticatorInterceptor = OkHttpAuthenticatorInterceptor(authority)
                val okHttpClient = okHttpClient.newBuilder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.HEADERS
                        }
                    )
                    .addNetworkInterceptor(authenticatorInterceptor)
                    .authenticator(authenticatorInterceptor)
                    .build()
                client = OneDriveNetwork(okHttpClient)
                clients[authority] = client
            }
            return client
        }
    }

    suspend fun getRootFileList(authority: ServerAuthority): ApiOperation<List<CloudFileItem>> =
        getClient(authority).getRootFileList()

    suspend fun getFileList(
        directoryId: String,
        authority: ServerAuthority
    ): ApiOperation<List<CloudFileItem>> = getClient(authority).getFileList(directoryId)

    suspend fun createDirectory(
        name: String,
        parentId: String,
        authority: ServerAuthority
    ): ApiOperation<CloudFileItem> = getClient(authority).createDirectory(name, parentId)

    suspend fun uploadFile(
        file: File,
        parentId: String,
        authority: ServerAuthority
    ): ApiOperation<CloudFileItem> = getClient(authority).uploadFile(file, parentId)

    fun uploadFile(
        path: Path,
        parentId: String,
        authority: ServerAuthority
    ): Flow<TransferStatus> = flow {
        val client = getClient(authority)
        val fileSize = path.size()
        if (fileSize > 1024 * 1024 * 5) {
            val response = client.getUploadUrl(parentId, path.fileName.toString())
            when (response) {
                is ApiOperation.Success -> {
                    response.data?.let {
                        Log.d("chengluo", "uploadFile getUploadUrl: $it")
                        client.uploadFileInChunks(it, path).collect { emit(it) }
                    }
                }

                is ApiOperation.Failure -> {
                    Log.e("chengluo", "uploadFile: ", response.exception)
                    emit(TransferStatus.Failed(response.exception))
                }
            }
        } else {
            client.uploadFile(path, parentId)
            emit(TransferStatus.Finished())
        }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    suspend fun getDownloadUrl(fileId: String, authority: ServerAuthority): ApiOperation<String> =
        getClient(authority).getDownloadUrl(fileId)

    suspend fun downloadFile(
        url: String,
        fileSize: Long,
        path: Path,
        authority: ServerAuthority
    ): Flow<TransferStatus> = getClient(authority).downloadFile(url, fileSize, path)

    suspend fun renameFile(
        fileId: String,
        newName: String,
        authority: ServerAuthority
    ): ApiOperation<CloudFileItem> = getClient(authority).renameFile(fileId, newName)

    suspend fun deleteFileById(fileId: String, authority: ServerAuthority): ApiOperation<Unit> =
        getClient(authority).deleteFileById(fileId)

    suspend fun copyFile(
        fileId: String,
        targetFileId: String,
        authority: ServerAuthority
    ): ApiOperation<Unit> = getClient(authority).copyFile(fileId, targetFileId)

    suspend fun moveFile(
        fileId: String,
        targetFileId: String,
        authority: ServerAuthority
    ): ApiOperation<CloudFileItem> = getClient(authority).moveFile(fileId, targetFileId)

    internal class OkHttpAuthenticatorInterceptor(
        private val authority: ServerAuthority
    ) : AuthenticatorInterceptor {

        private fun getAuthenticatorInterceptor(): AuthenticatorInterceptor =
            OneDriveAuthentication.createAuthenticatorInterceptor(authority)

        override fun authenticate(route: Route?, response: Response): Request? =
            getAuthenticatorInterceptor().authenticate(route, response)

        override fun intercept(chain: Interceptor.Chain): Response =
            getAuthenticatorInterceptor().intercept(chain)
    }
}