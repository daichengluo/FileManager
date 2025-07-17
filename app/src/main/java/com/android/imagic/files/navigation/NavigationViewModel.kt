package com.android.imagic.files.navigation

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.imagic.files.cloud.gdrive.sdk.GoogleDriveServiceClient
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.storage.StarBoardDriveServer
import com.android.imagic.files.storage.Storages
import com.android.imagic.files.util.valueCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn

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
}