package com.android.imagic.files.filelist

// TODO: Make immutable?
class CloudPasteState(
    var copy: Boolean = false,
    var cloudMode: FileModeState? = null,
    val files: CloudFileItemSet = cloudFileItemSetOf()
)
