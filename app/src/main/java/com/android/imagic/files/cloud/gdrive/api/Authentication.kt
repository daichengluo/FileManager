package com.android.imagic.files.cloud.gdrive.api

import android.os.Parcelable
import com.android.imagic.files.cloud.util.AuthenticatorInterceptor
import com.android.imagic.files.cloud.util.ServerAuthority
import kotlinx.parcelize.Parcelize
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

sealed class Authentication : Parcelable {
    abstract fun createAuthenticatorInterceptor(authority: ServerAuthority): AuthenticatorInterceptor
}

@Parcelize
data object GoogleDriveAuthentication : Authentication() {

    override fun createAuthenticatorInterceptor(authority: ServerAuthority): AuthenticatorInterceptor =
        object : AuthenticatorInterceptor {

            override fun authenticate(route: Route?, response: Response): Request? = null

            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${authority.accessToken}")
                    .build()

                return chain.proceed(request)
            }
        }
}
