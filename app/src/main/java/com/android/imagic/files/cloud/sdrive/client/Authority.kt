package com.android.imagic.files.cloud.sdrive.client

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.security.MessageDigest
import java.time.Clock

@Parcelize
data class Authority(
    val openId: String,
    val accessToken: String,
    val userId: Int,
    val username: String,
    val headerImageUrl: String? = null
) : Parcelable {
    val signTimestamp: Pair<String, String>
        get() = generateSign()

    private fun generateSign(
        salt: String = "3875c186acd8976c537f91465ede2269"
    ): Pair<String, String> {
        val timestamp = Clock.systemUTC().instant().epochSecond.toString()
        return sha256(openId + timestamp + accessToken + salt) to timestamp
    }

    companion object {
        fun sha256(input: String): String = MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
