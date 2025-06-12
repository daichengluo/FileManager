package com.android.imagic.files.cloud.sdrive.client.exception

import com.android.imagic.files.cloud.sdrive.client.ApiErrorCode

class LoginExpiredException(message: String?) :
    StarBoardDriveException(message, ApiErrorCode.LOGIN_EXPIRED)