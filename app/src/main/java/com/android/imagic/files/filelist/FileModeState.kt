package com.android.imagic.files.filelist

import com.android.imagic.files.file.QuickAccessWrap
import com.android.imagic.files.provider.cloud.sdrive.client.Authority

sealed interface FileModeState {
    data object Local : FileModeState
    data class QuickAccess(val quickAccessWrap: QuickAccessWrap) : FileModeState
    data class Cloud(val authority: Authority) : FileModeState
}