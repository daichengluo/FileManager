package com.android.imagic.files.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.imagic.files.storage.StarBoardDriveServer
import com.android.imagic.files.util.valueCompat

class NavigationViewModel : ViewModel() {

    private val _starBoardDriveServer = MutableLiveData<StarBoardDriveServer?>(null)
    val starBoardDriveServer: LiveData<StarBoardDriveServer?> = _starBoardDriveServer

    fun updateServer(server: StarBoardDriveServer?) {
        if (_starBoardDriveServer.valueCompat != server) {
            _starBoardDriveServer.value = server
        }
    }
}