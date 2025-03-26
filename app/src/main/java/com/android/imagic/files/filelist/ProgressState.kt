package com.android.imagic.files.filelist

data class ProgressState(
    val id: Int = -1,
    val title: String? = null,
    val text: String? = null,
    val max: Int = 100,
    val progress: Int = 0,
    val indeterminate: Boolean = false
) {
    val isTransferring: Boolean
        get() = progress < max

    val isFinished: Boolean
        get() = progress >= max
}
