package com.android.imagic.files.filelist

import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.file.QuickAccessWrap

sealed interface FileModeState {
    data object Local : FileModeState
    data class QuickAccess(val quickAccessWrap: QuickAccessWrap) : FileModeState
    data class Cloud(val authority: Authority) : FileModeState
    data class GoogleDrive(val accessToken: String) : FileModeState
    data class OneDrive(val accessToken: String) : FileModeState
}