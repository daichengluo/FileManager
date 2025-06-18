package com.android.imagic.files.cloud.util

import android.content.Context
import android.os.Parcelable
import com.android.imagic.files.R
import com.android.imagic.files.filelist.CloudPathData
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServerAuthority(
    val openId: String,
    val userId: Int,
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val accessToken: String,
    val refreshToken: String,
    val expireAt: Long,
    val type: Int,
    val totalSpace: Long,
    val usedSpace: Long
) : Parcelable {

    val isTokenExpired: Boolean
        get() {
            return System.currentTimeMillis() >= expireAt - 5 * 60 * 1000 // 提前 5 分钟刷新
        }

    val remaining: Long
        get() = totalSpace - usedSpace

    val isGoogle: Boolean
        get() = type == 1

    val isMicrosoft: Boolean
        get() = type == 2

    fun getRootPath(context: Context): CloudPathData = when (type) {
        1 -> CloudPathData("root", context.getString(R.string.my_drive))
        2 -> CloudPathData("root", context.getString(R.string.my_files))
        else -> CloudPathData("0", context.getString(R.string.my_files))
    }

    fun getTitle(context: Context): String = when (type) {
        1 -> context.getString(R.string.google_drive)
        2 -> context.getString(R.string.one_drive)
        else -> context.getString(R.string.starboard_drive)
    }
}
