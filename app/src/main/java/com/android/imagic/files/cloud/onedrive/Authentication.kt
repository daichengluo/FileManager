package com.android.imagic.files.cloud.onedrive

import android.os.Parcelable
import com.android.imagic.files.appwidget.toServerAuthority
import com.android.imagic.files.cloud.sdrive.client.StarBoardDriveClient
import com.android.imagic.files.cloud.util.AuthenticatorInterceptor
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.storage.OneDriveServer
import com.android.imagic.files.storage.StarBoardDriveServer
import com.android.imagic.files.storage.Storages
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.valueCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

sealed class Authentication : Parcelable {
    abstract fun createAuthenticatorInterceptor(authority: ServerAuthority): AuthenticatorInterceptor
}

@Parcelize
data object OneDriveAuthentication : Authentication() {

    override fun createAuthenticatorInterceptor(authority: ServerAuthority): AuthenticatorInterceptor =
        object : AuthenticatorInterceptor {

            override fun authenticate(route: Route?, response: Response): Request? {
                if (responseCount(response) >= 2) return null

                val newToken = runBlocking { refreshAccessToken(authority) } ?: return null

                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }

            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${authority.accessToken}")
                    .build()

                return chain.proceed(request)
            }

            private fun responseCount(response: Response): Int {
                var r = response
                var count = 1
                while (r.priorResponse != null) {
                    count++
                    r = r.priorResponse!!
                }
                return count
            }

            private suspend fun refreshAccessToken(serverAuthority: ServerAuthority): String? =
                withContext(Dispatchers.IO) {
                    val storages = Settings.STORAGES.valueCompat.toList()
                    val starBoardDriveServers = storages.firstOrNull { it is StarBoardDriveServer }
                    starBoardDriveServers?.let { server ->
                        val starBoardDriveServer = server as StarBoardDriveServer
                        val authority = starBoardDriveServer.authority
                        val response = StarBoardDriveClient.refreshAccessToken(
                            serverAuthority.type,
                            serverAuthority.refreshToken,
                            authority
                        )
                        if (response is ApiOperation.Success) {
                            val data = response.data
                            val oneDriveServer = storages.firstOrNull { it is OneDriveServer }
                            val server = if (oneDriveServer == null) {
                                OneDriveServer(
                                    null,
                                    null,
                                    data.toServerAuthority()
                                )
                            } else {
                                OneDriveServer(
                                    oneDriveServer.id,
                                    oneDriveServer.customName,
                                    data.toServerAuthority()
                                )
                            }
                            Storages.addOrReplace(server)
                            return@withContext data.access_token
                        } else {
                            return@withContext null
                        }
                    }
                    return@withContext null
                }
        }
}
