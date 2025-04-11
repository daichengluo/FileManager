package com.android.imagic.files.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuickAccessSettings(
    val id: String,
    val customTitle: String?,
    val category: QuickAccessCategory,
    val isEnabled: Boolean
) : Parcelable
