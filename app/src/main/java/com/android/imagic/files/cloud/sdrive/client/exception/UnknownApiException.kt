package com.android.imagic.files.cloud.sdrive.client.exception

import com.android.imagic.files.cloud.sdrive.client.ApiErrorCode

class UnknownApiException(message: String?, code: String?) :
    StarBoardDriveException(message, ApiErrorCode.from(code))