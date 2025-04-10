package com.android.imagic.files.navigation

import android.os.Parcelable
import com.android.imagic.files.file.Category
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuickAccessSettings(
    val id: String,
    val customTitle: String?,
    val category: Category?,
    val isEnabled: Boolean
) : Parcelable
