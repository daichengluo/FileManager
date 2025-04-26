package com.android.imagic.files.provider.cloud.sdrive.client

enum class ApiErrorCode(val code: String) {
    LOGIN_EXPIRED("A0002"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun from(code: String?): ApiErrorCode {
            return ApiErrorCode.entries.find { it.code == code } ?: UNKNOWN
        }
    }
}