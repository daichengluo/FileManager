package com.android.imagic.files.cloud.onedrive

import android.util.Log
import com.android.imagic.files.app.okHttpClient
import com.android.imagic.files.cloud.CloudFileItem
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

    private val clients = mutableMapOf<String, OneDriveNetworkClient>()

    private fun getClient(accessToken: String): OneDriveNetworkClient {
        synchronized(clients) {
            var client = clients[accessToken]
            if (client == null) {
                val authenticatorInterceptor = OkHttpAuthenticatorInterceptor(accessToken)
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
                client = OneDriveNetworkClient(okHttpClient)
                clients[accessToken] = client
            }
            return client
        }
    }

    suspend fun getRootFileList(accessToken: String): ApiOperation<List<CloudFileItem>> =
        getClient(accessToken).getRootFileList()

    suspend fun getFileList(
        directoryId: String,
        accessToken: String
    ): ApiOperation<List<CloudFileItem>> = getClient(accessToken).getFileList(directoryId)

    suspend fun createDirectory(
        name: String,
        parentId: String,
        accessToken: String
    ): ApiOperation<CloudFileItem> = getClient(accessToken).createDirectory(name, parentId)

    suspend fun uploadFile(
        file: File,
        parentId: String,
        accessToken: String
    ): ApiOperation<CloudFileItem> = getClient(accessToken).uploadFile(file, parentId)

    fun uploadFile(
        path: Path,
        parentId: String,
        accessToken: String
    ): Flow<TransferStatus> = flow {
        val client = getClient(accessToken)
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

    suspend fun getDownloadUrl(fileId: String, accessToken: String): ApiOperation<String> =
        getClient(accessToken).getDownloadUrl(fileId)

    suspend fun downloadFile(
        url: String,
        fileSize: Long,
        path: Path,
        accessToken: String
    ): Flow<TransferStatus> = getClient(accessToken).downloadFile(url, fileSize, path)

    suspend fun renameFile(
        fileId: String,
        newName: String,
        accessToken: String
    ): ApiOperation<CloudFileItem> = getClient(accessToken).renameFile(fileId, newName)

    suspend fun deleteFileById(fileId: String, accessToken: String): ApiOperation<Unit> =
        getClient(accessToken).deleteFileById(fileId)

    suspend fun copyFile(
        fileId: String,
        targetFileId: String,
        accessToken: String
    ): ApiOperation<Unit> = getClient(accessToken).copyFile(fileId, targetFileId)

    suspend fun moveFile(
        fileId: String,
        targetFileId: String,
        accessToken: String
    ): ApiOperation<CloudFileItem> = getClient(accessToken).moveFile(fileId, targetFileId)

    internal class OkHttpAuthenticatorInterceptor(
        private val accessToken: String
    ) : AuthenticatorInterceptor {

        private fun getAuthenticatorInterceptor(): AuthenticatorInterceptor =
            OneDriveAuthentication.createAuthenticatorInterceptor(accessToken)

        override fun authenticate(route: Route?, response: Response): Request? =
            getAuthenticatorInterceptor().authenticate(route, response)

        override fun intercept(chain: Interceptor.Chain): Response =
            getAuthenticatorInterceptor().intercept(chain)
    }
}