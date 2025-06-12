package com.android.imagic.files.filelist

data class CloudPathData(
    val directoryId: String,
    val name: String
) {
    val isRoot: Boolean
        get() = directoryId == "root" || directoryId == "0"
}
