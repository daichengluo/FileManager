package com.android.imagic.files.file

import android.content.Context

data class QuickAccessWrap(
    val context: Context,
    val title: String,
    val fileTypeCategory: FileTypeCategory
)
