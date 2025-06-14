package com.android.imagic.files.navigation

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.storage.StarBoardDriveServer
import com.android.imagic.files.util.valueCompat

class NavigationViewModel : ViewModel() {

    private val _starBoardDriveServer = MutableLiveData<StarBoardDriveServer?>(null)
    val starBoardDriveServer: LiveData<StarBoardDriveServer?> = _starBoardDriveServer

    private val _serverAuthority = MutableLiveData<ServerAuthority?>(null)
    val serverAuthority: LiveData<ServerAuthority?> = _serverAuthority

    fun updateServer(server: StarBoardDriveServer?) {
        if (_starBoardDriveServer.valueCompat != server) {
            _starBoardDriveServer.value = server
        }
    }

    fun queryAuthority(context: Context) {
        context.contentResolver.query(
            "content://com.android.imagic.authentication.auth_provider/authority".toUri(),
            null, null, null, null
        )?.use {
            if (it.moveToFirst()) {
                val openId = it.getString(it.getColumnIndexOrThrow("openId"))
                val userId = it.getInt(it.getColumnIndexOrThrow("userId"))
                val username = it.getString(it.getColumnIndexOrThrow("username"))
                val headImageUrl = it.getString(it.getColumnIndexOrThrow("headImageUrl"))
                val accessToken = it.getString(it.getColumnIndexOrThrow("accessToken"))
                val refreshToken = it.getString(it.getColumnIndexOrThrow("refreshToken"))
                val expireAt = it.getLong(it.getColumnIndexOrThrow("expireAt"))
                val type = it.getInt(it.getColumnIndexOrThrow("type"))

                val serverAuthority = ServerAuthority(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expireAt = expireAt,
                    type = type
                )
                _serverAuthority.value = serverAuthority
            }
        }
    }
}