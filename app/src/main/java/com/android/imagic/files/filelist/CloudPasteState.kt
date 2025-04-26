package com.android.imagic.files.filelist

// TODO: Make immutable?
class CloudPasteState(
    var copy: Boolean = false,
    val files: CloudFileItemSet = cloudFileItemSetOf()
)
