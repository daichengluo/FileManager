/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package com.android.imagic.files.filelist

import com.android.imagic.files.file.MimeType

class PickOptions(
    val mode: Mode,
    val fileName: String?,
    val readOnly: Boolean,
    val mimeTypes: List<MimeType>,
    val localOnly: Boolean,
    val allowMultiple: Boolean
) {
    enum class Mode {
        OPEN_FILE,
        CREATE_FILE,
        OPEN_DIRECTORY
    }
}
