package com.android.imagic.files.cloud.util

import okhttp3.Authenticator
import okhttp3.Interceptor

interface AuthenticatorInterceptor : Authenticator, Interceptor