package com.android.imagic.files.cloud.sdrive.client

import com.android.imagic.files.app.okHttpClient
import com.android.imagic.files.appwidget.AccessTokenData
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.util.AuthenticatorInterceptor
import com.android.imagic.files.cloud.util.TransferStatus
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

object StarBoardDriveClient {

    private val clients = mutableMapOf<Authority, StarBoardDriveNetwork>()

    private fun getClient(authority: Authority): StarBoardDriveNetwork {
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
                client = StarBoardDriveNetwork(okHttpClient)
                clients[authority] = client
            }
            return client
        }
    }

    suspend fun createDirectory(
        name: String,
        parentId: String,
        authority: Authority
    ): ApiOperation<CloudFileItem> = getClient(authority).createDirectory(name, parentId)

    suspend fun renameFile(
        newName: String,
        fileId: String,
        authority: Authority
    ): ApiOperation<Boolean> = getClient(authority).renameFile(newName, fileId)

    suspend fun uploadFileInfo(
        name: String,
        size: Long,
        url: String,
        directoryId: String,
        authority: Authority
    ): ApiOperation<Boolean> = getClient(authority).uploadFileInfo(name, size, url, directoryId)

    suspend fun uploadFile(file: File, authority: Authority): ApiOperation<UploadFileInfo> =
        getClient(authority).uploadFile(authority.userId, file)

    suspend fun uploadFile(path: Path, authority: Authority): ApiOperation<UploadFileInfo> =
        getClient(authority).uploadFile(authority.userId, path)

    suspend fun uploadChunks(path: Path, authority: Authority): Flow<TransferStatus> =
        getClient(authority).uploadChunks(authority.userId, path, 256 * 1024L)

    suspend fun downloadFile(url: String, path: Path, authority: Authority): Flow<TransferStatus> =
        getClient(authority).downloadFile(url, path)

    suspend fun delete(fileIds: List<String>, authority: Authority): ApiOperation<Boolean> =
        getClient(authority).deleteFiles(fileIds.joinToString(","))

    suspend fun moveFile(fileIds: List<String>, targetId: String, authority: Authority) =
        getClient(authority).moveFiles(fileIds.joinToString(","), targetId)

    suspend fun copyFile(fileIds: List<String>, targetId: String, authority: Authority) =
        getClient(authority).copyFiles(fileIds.joinToString(","), targetId)

    suspend fun getFileList(
        parentId: String,
        authority: Authority
    ): ApiOperation<List<CloudFileItem>> = getClient(authority).getFileList(parentId)

    suspend fun getSpaceData(authority: Authority) = getClient(authority).getSpaceData()

    suspend fun refreshAccessToken(
        type: Int,
        refreshToken: String,
        authority: Authority
    ): ApiOperation<AccessTokenData> = getClient(authority).refreshAccessToken(type, refreshToken)

    internal class OkHttpAuthenticatorInterceptor(
        private val authority: Authority
    ) : AuthenticatorInterceptor {

        private fun getAuthenticatorInterceptor(): AuthenticatorInterceptor =
            StarBoardDriveAuthentication.createAuthenticatorInterceptor(authority)

        override fun authenticate(route: Route?, response: Response): Request? =
            getAuthenticatorInterceptor().authenticate(route, response)

        override fun intercept(chain: Interceptor.Chain): Response =
            getAuthenticatorInterceptor().intercept(chain)
    }
}