package com.android.imagic.files.navigation

import android.os.Parcelable
import com.android.imagic.files.file.MimeTypeCategory
import kotlinx.parcelize.Parcelize

@Parcelize
data class StandardDirectorySettings(
    val id: String,
    val customTitle: String?,
    val category: MimeTypeCategory?,
    val isEnabled: Boolean
) : Parcelable
