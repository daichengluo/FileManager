package com.android.imagic.files.provider.webdav.client

import android.os.Parcelable
import com.android.imagic.files.provider.common.UriAuthority
import com.android.imagic.files.util.takeIfNotEmpty
import kotlinx.parcelize.Parcelize

@Parcelize
data class Authority(
    val protocol: Protocol,
    val host: String,
    val port: Int,
    val username: String
) : Parcelable {
    fun toUriAuthority(): UriAuthority {
        val userInfo = username.takeIfNotEmpty()
        val uriPort = port.takeIf { it != protocol.defaultPort }
        return UriAuthority(userInfo, host, uriPort)
    }

    override fun toString(): String = toUriAuthority().toString()
}
