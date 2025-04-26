package com.android.imagic.files.provider.cloud.sdrive.client.exception

import com.android.imagic.files.provider.cloud.sdrive.client.ApiErrorCode

class LoginExpiredException(message: String?) : SDriveException(message, ApiErrorCode.LOGIN_EXPIRED)