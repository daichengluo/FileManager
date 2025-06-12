package com.android.imagic.files.cloud.sdrive.client.exception

import com.android.imagic.files.cloud.sdrive.client.ApiErrorCode

open class StarBoardDriveException(message: String?, val code: ApiErrorCode) : Exception(message)