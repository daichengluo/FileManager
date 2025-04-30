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
import java.io.File

object Client {

    private val clients = mutableMapOf<Authority, SDriveNetworkClient>()

    private fun getClient(authority: Authority): SDriveNetworkClient {
        synchronized(clients) {
            var client = clients[authority]
            if (client == null) {
                val authenticatorInterceptor = OkHttpAuthenticatorInterceptor(authority)
                val okHttpClient = okHttpClient.newBuilder()
                    .followRedirects(false)
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
    ): ApiOperation<Boolean> = getClient(authority).createDirectory(name, parentId)

    suspend fun renameFile(
        newName: String,
        fileId: Int,
        authority: Authority
    ): ApiOperation<Boolean> = getClient(authority).renameFile(newName, fileId)

    suspend fun uploadFile(
        userId: Int,
        file: File,
        targetDirectoryId: Int,
        authority: Authority
    ): ApiOperation<Boolean> {
        val response = getClient(authority).uploadFile(userId, file)
        return when (response) {
            is ApiOperation.Failure -> response as ApiOperation.Failure<Boolean>
            is ApiOperation.Success -> {
                val fileInfo = response.data
                val body = FileInfoBody(
                    ctFileName = file.name,
                    fileSize = file.length().toInt(),
                    filePath = fileInfo.url ?: "",
                    fileHash = "",
                    targetID = targetDirectoryId
                )
                getClient(authority).uploadFileInfo(body)
            }
        }
    }

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