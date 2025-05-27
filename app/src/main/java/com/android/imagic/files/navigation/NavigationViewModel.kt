package com.android.imagic.files.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.imagic.files.storage.SDriveServer
import com.android.imagic.files.util.valueCompat

class NavigationViewModel : ViewModel() {

    private val _sDriveServer = MutableLiveData<SDriveServer?>(null)
    val sDriveServer: LiveData<SDriveServer?> = _sDriveServer

    fun updateServer(server: SDriveServer?) {
        if (_sDriveServer.valueCompat != server) {
            _sDriveServer.value = server
        }
    }
}