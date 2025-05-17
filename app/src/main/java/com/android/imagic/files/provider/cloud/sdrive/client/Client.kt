package com.android.imagic.files.provider.cloud.sdrive.client

import com.android.imagic.files.app.okHttpClient
import com.android.imagic.files.file.cloud.sdrive.CloudFileItem
import com.android.imagic.files.util.ApiOperation
import java8.nio.file.Path
import kotlinx.coroutines.flow.Flow
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

object Client {

    private val clients = mutableMapOf<Authority, SDriveNetworkClient>()

    private fun getClient(authority: Authority): SDriveNetworkClient {
        synchronized(clients) {
            var client = clients[authority]
            if (client == null) {
                val authenticatorInterceptor = OkHttpAuthenticatorInterceptor(authority)
                val okHttpClient = okHttpClient.newBuilder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .followRedirects(false)
                    .addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.HEADERS
                        }
                    )
                    .addNetworkInterceptor(authenticatorInterceptor)
                    .authenticator(authenticatorInterceptor)
                    .build()
                client = SDriveNetworkClient(okHttpClient)
                clients[authority] = client
            }
            return client
        }
    }

    suspend fun createDirectory(
        name: String,
        parentId: Int,
        authority: Authority
    ): ApiOperation<CloudFileItem> = getClient(authority).createDirectory(name, parentId)

    suspend fun renameFile(
        newName: String,
        fileId: Int,
        authority: Authority
    ): ApiOperation<Boolean> = getClient(authority).renameFile(newName, fileId)

    suspend fun uploadFileInfo(
        name: String,
        size: Int,
        url: String,
        directoryId: Int,
        authority: Authority
    ): ApiOperation<Boolean> = getClient(authority).uploadFileInfo(name, size, url, directoryId)

    suspend fun uploadFile(file: File, authority: Authority): ApiOperation<UploadFileInfo> =
        getClient(authority).uploadFile(authority.userId, file)

    suspend fun uploadFile(path: Path, authority: Authority): ApiOperation<UploadFileInfo> =
        getClient(authority).uploadFile(authority.userId, path)

    suspend fun uploadChunks(path: Path, authority: Authority): Flow<TransferStatus> =
        getClient(authority).uploadChunks(authority.userId, path)

    suspend fun downloadFile(url: String, path: Path, authority: Authority): Flow<TransferStatus> =
        getClient(authority).downloadFile(url, path)

    suspend fun delete(fileIds: List<Int>, authority: Authority): ApiOperation<Boolean> =
        getClient(authority).deleteFiles(fileIds.joinToString(","))

    suspend fun move(fileIds: List<Int>, targetId: Int, authority: Authority) =
        getClient(authority).moveFiles(fileIds.joinToString(","), targetId)

    suspend fun copy(fileIds: List<Int>, targetId: Int, authority: Authority) =
        getClient(authority).copyFiles(fileIds.joinToString(","), targetId)

    suspend fun getFileList(
        parentId: Int,
        authority: Authority
    ): ApiOperation<List<CloudFileItem>> = getClient(authority).getFileList(parentId)

    internal class OkHttpAuthenticatorInterceptor(
        private val authority: Authority
    ) : AuthenticatorInterceptor {

        private fun getAuthenticatorInterceptor(): AuthenticatorInterceptor =
            SDriveAuthentication.createAuthenticatorInterceptor(authority)

        override fun authenticate(route: Route?, response: Response): Request? =
            getAuthenticatorInterceptor().authenticate(route, response)

        override fun intercept(chain: Interceptor.Chain): Response =
            getAuthenticatorInterceptor().intercept(chain)
    }
}