package com.android.imagic.files.cloud.gdrive

import com.android.imagic.files.app.okHttpClient
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object GoogleDriveClient {

    private val clients = mutableMapOf<String, GoogleDriveServiceClient>()

    private fun getClient(accessToken: String): GoogleDriveServiceClient {
        synchronized(clients) {
            var client = clients[accessToken]
            if (client == null) {
                val authenticatorInterceptor = OkHttpAuthenticatorInterceptor(accessToken)
                val okHttpClient = okHttpClient.newBuilder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .followRedirects(false)
                    .addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                    .addNetworkInterceptor(authenticatorInterceptor)
                    .authenticator(authenticatorInterceptor)
                    .build()
                client = GoogleDriveServiceClient(okHttpClient)
                clients[accessToken] = client
            }
            return client
        }
    }

    suspend fun getRootDirectoryFiles(accessToken: String) =
        getClient(accessToken).getRootDirectoryFiles()

    suspend fun createFolder(accessToken: String, folderName: String) =
        getClient(accessToken).createFolder(folderName)

    internal class OkHttpAuthenticatorInterceptor(
        private val accessToken: String
    ) : AuthenticatorInterceptor {

        private fun getAuthenticatorInterceptor(): AuthenticatorInterceptor =
            GoogleDriveAuthentication.createAuthenticatorInterceptor(accessToken)

        override fun authenticate(route: Route?, response: Response): Request? =
            getAuthenticatorInterceptor().authenticate(route, response)

        override fun intercept(chain: Interceptor.Chain): Response =
            getAuthenticatorInterceptor().intercept(chain)
    }
}