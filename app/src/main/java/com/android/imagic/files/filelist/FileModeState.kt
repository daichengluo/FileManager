package com.android.imagic.files.filelist

import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.file.QuickAccessWrap

sealed interface FileModeState {
    data object Local : FileModeState
    data class QuickAccess(val quickAccessWrap: QuickAccessWrap) : FileModeState
    data class StarBoardDrive(val authority: Authority) : FileModeState
    data class GoogleDrive(val authority: ServerAuthority) : FileModeState
    data class OneDrive(val authority: ServerAuthority) : FileModeState
}