package com.android.imagic.files.cloud.sdrive.client

import android.content.Context
import android.os.Parcelable
import com.android.imagic.files.R
import com.android.imagic.files.filelist.CloudPathData
import kotlinx.parcelize.Parcelize
import java.security.MessageDigest
import java.time.Clock

@Parcelize
data class Authority(
    val openId: String,
    val accessToken: String,
    val userId: Int,
    val username: String,
    val headerImageUrl: String? = null,
    val totalSpace: Long = 5242880,
    val freeSpace: Long = 5242880
) : Parcelable {

    val usedSpace: Long
        get() = totalSpace - freeSpace

    val signTimestamp: Pair<String, String>
        get() = generateSign()

    private fun generateSign(
        salt: String = "3875c186acd8976c537f91465ede2269"
    ): Pair<String, String> {
        val timestamp = Clock.systemUTC().instant().epochSecond.toString()
        return sha256(openId + timestamp + accessToken + salt) to timestamp
    }

    fun getRootPath(context: Context): CloudPathData =
        CloudPathData("0", context.getString(R.string.my_files))

    fun getTitle(context: Context): String = context.getString(R.string.starboard_drive)

    companion object {
        fun sha256(input: String): String = MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
