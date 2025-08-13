package com.android.imagic.files.cloud.sdrive.client

import android.os.Parcelable
import com.android.imagic.files.cloud.util.AuthenticatorInterceptor
import kotlinx.parcelize.Parcelize
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

sealed class Authentication : Parcelable {
    abstract fun createAuthenticatorInterceptor(authority: Authority): AuthenticatorInterceptor
}

@Parcelize
data object StarBoardDriveAuthentication : Authentication() {

    override fun createAuthenticatorInterceptor(authority: Authority): AuthenticatorInterceptor =
        object : AuthenticatorInterceptor {
            override fun authenticate(route: Route?, response: Response): Request? = null

            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request()
                val (sign, timestamp) = authority.signTimestamp
                val newRequest = request.newBuilder()
                    .header("Authorization", authority.accessToken)
                    .header("openid", authority.openId)
                    .header("sign", sign)
                    .header("timestamp", timestamp)
                    .header("device", "9")
                    .build()

                return chain.proceed(newRequest)
            }
        }
}
