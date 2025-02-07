/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package com.android.imagic.files.filelist

// TODO: Make immutable?
class PasteState(
    var copy: Boolean = false,
    val files: FileItemSet = fileItemSetOf()
)
