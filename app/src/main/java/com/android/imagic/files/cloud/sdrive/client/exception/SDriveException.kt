package com.android.imagic.files.cloud.sdrive.client.exception

import com.android.imagic.files.cloud.sdrive.client.ApiErrorCode

open class SDriveException(message: String?, val code: ApiErrorCode) : Exception(message)