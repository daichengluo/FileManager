package com.android.imagic.files.provider.cloud.sdrive.client.exception

import com.android.imagic.files.provider.cloud.sdrive.client.ApiErrorCode

class UnknownApiException(message: String?, code: String?) :
    SDriveException(message, ApiErrorCode.from(code))