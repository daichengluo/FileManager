package com.android.imagic.files.cloud.util

data class SpaceData(
    val total: Long,
    val used: Long
) {
    val remaining: Long
        get() = total - used
}
