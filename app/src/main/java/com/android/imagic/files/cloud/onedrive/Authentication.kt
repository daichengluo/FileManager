package com.android.imagic.files.cloud.onedrive

import android.os.Parcelable
import android.util.Log
import com.android.imagic.files.app.okHttpClient
import com.android.imagic.files.appwidget.CloudClient
import com.android.imagic.files.cloud.util.AuthenticatorInterceptor
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.storage.OneDriveServer
import com.android.imagic.files.storage.Storages
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.valueCompat
import kotlinx.coroutines.runBlocking
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

            private fun responseCount(response: Response): Int {
                var res = response
                var count = 1
                while (res.priorResponse != null) {
                    count++
                    res = res.priorResponse!!
                }
                return count
            }

            private suspend fun refreshTokenIfNeeded(authority: ServerAuthority): Boolean {
                return if (authority.isTokenExpired) {
                    Log.d("chengluo", "refreshToken old authority: $authority")
                    val response =
                        CloudClient(okHttpClient.newBuilder().build()).refreshAccessToken(
                            authority.type,
                            authority.refreshToken
                        )
                    if (response is ApiOperation.Success) {
                        val newAuthority = response.data
                        Log.d("chengluo", "refreshToken Success newAuthority: $newAuthority")
                        val storages = Settings.STORAGES.valueCompat.toList()
                        val oneDriveServers = storages.filter { it is OneDriveServer }
                        oneDriveServers.firstOrNull()?.let { server ->
                            val oneDriveServer = server as OneDriveServer
                            if (oneDriveServer.authority == authority) {
                                val newServer = OneDriveServer(
                                    oneDriveServer.id,
                                    oneDriveServer.customName,
                                    newAuthority
                                )
                                Storages.addOrReplace(newServer)
                                return true
                            }
                        }
                        return false
                    } else false
                } else false
            }

            override fun authenticate(route: Route?, response: Response): Request? {
                if (responseCount(response) >= 2) return null
                val refreshed = runBlocking { refreshTokenIfNeeded(authority) }

                val storages = Settings.STORAGES.valueCompat.toList()
                val oneDriveServers = storages.filter { it is OneDriveServer }
                val server = oneDriveServers.firstOrNull() ?: return null
                val accessToken = (server as OneDriveServer).authority.accessToken

                return if (refreshed) {
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                } else {
                    null
                }
            }

            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${authority.accessToken}")
                    .build()

                return chain.proceed(request)
            }
        }
}
