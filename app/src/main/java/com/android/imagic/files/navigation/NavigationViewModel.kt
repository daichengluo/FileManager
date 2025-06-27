package com.android.imagic.files.navigation

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.imagic.files.cloud.gdrive.api.GoogleDriveClient
import com.android.imagic.files.cloud.gdrive.sdk.GoogleDriveServiceClient
import com.android.imagic.files.cloud.onedrive.OneDriveClient
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.storage.StarBoardDriveServer
import com.android.imagic.files.storage.Storages
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.valueCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.launch

class NavigationViewModel : ViewModel() {

    private val _starBoardDriveServer = MutableLiveData<StarBoardDriveServer?>(null)
    val starBoardDriveServer: LiveData<StarBoardDriveServer?> = _starBoardDriveServer

    private val _serverAuthority = MutableLiveData<ServerAuthority?>(null)
    val serverAuthority: LiveData<ServerAuthority?> = _serverAuthority

    fun onStorageItemsChanged(navigationItems: List<NavigationItem?>, context: Context) {
        navigationItems.firstOrNull { it is StarBoardDriveItem }?.let { item ->
            val server = (item as StarBoardDriveItem).storage
            if (_starBoardDriveServer.valueCompat != server) {
                _starBoardDriveServer.value = server
            }
        } ?: run { _starBoardDriveServer.value = null }
        navigationItems.firstOrNull { it is GoogleDriveItem }?.let { item ->
            val server = (item as GoogleDriveItem).storage
            val authority = server.authority
            if (authority.accessToken.isEmpty()) {
                GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
                    GoogleDriveServiceClient.getService(server.authority, account, context)
                } ?: Storages.remove(server)
            }
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
                val email = it.getString(it.getColumnIndexOrThrow("email"))
                val avatarUrl = it.getString(it.getColumnIndexOrThrow("avatarUrl"))
                val accessToken = it.getString(it.getColumnIndexOrThrow("accessToken"))
                val refreshToken = it.getString(it.getColumnIndexOrThrow("refreshToken"))
                val expireAt = it.getLong(it.getColumnIndexOrThrow("expireAt"))
                val type = it.getInt(it.getColumnIndexOrThrow("type"))
                val totalSpace = it.getLong(it.getColumnIndexOrThrow("totalSpace"))
                val usedSpace = it.getLong(it.getColumnIndexOrThrow("usedSpace"))

                val serverAuthority = ServerAuthority(
                    openId = openId,
                    userId = userId,
                    username = username,
                    email = email,
                    avatarUrl = avatarUrl,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expireAt = expireAt,
                    type = type,
                    totalSpace = totalSpace,
                    usedSpace = usedSpace
                )
                Log.d("chengluo", "queryAuthority: $serverAuthority")
                getDriveSpaceData(serverAuthority)
            }
        }
    }

    fun getDriveSpaceData(serverAuthority: ServerAuthority) = viewModelScope.launch {
        Log.d("chengluo", "getDriveSpaceData: $serverAuthority")
        if (serverAuthority.totalSpace == 0L) {
            if (serverAuthority.isGoogle) {
                if (serverAuthority.accessToken.isEmpty()) {
                    GoogleDriveServiceClient.getSpaceData(serverAuthority)?.let { spaceData ->
                        _serverAuthority.value = serverAuthority.copy(
                            totalSpace = spaceData.total,
                            usedSpace = spaceData.used
                        )
                    }
                } else {
                    val response = GoogleDriveClient.getSpaceData(serverAuthority)
                    if (response is ApiOperation.Success) {
                        val data = response.data
                        _serverAuthority.value = serverAuthority.copy(
                            totalSpace = data.total,
                            usedSpace = data.used
                        )
                    }
                }
            }
            if (serverAuthority.isMicrosoft) {
                val response = OneDriveClient.getSpaceData(serverAuthority)
                if (response is ApiOperation.Success) {
                    val data = response.data
                    _serverAuthority.value = serverAuthority.copy(
                        totalSpace = data.total,
                        usedSpace = data.used
                    )
                }
            }
        }
    }
}