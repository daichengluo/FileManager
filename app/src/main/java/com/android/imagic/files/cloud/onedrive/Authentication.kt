package com.android.imagic.files.cloud.onedrive

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

sealed class Authentication : Parcelable {
    abstract fun createAuthenticatorInterceptor(accessToken: String): AuthenticatorInterceptor
}

interface AuthenticatorInterceptor : Authenticator, Interceptor

@Parcelize
data object OneDriveAuthentication : Authentication() {

    override fun createAuthenticatorInterceptor(accessToken: String): AuthenticatorInterceptor =
        object : AuthenticatorInterceptor {
            override fun authenticate(route: Route?, response: Response): Request? = null

            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()

                return chain.proceed(request)
            }
        }
}
