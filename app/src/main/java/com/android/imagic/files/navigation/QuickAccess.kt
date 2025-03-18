package com.android.imagic.files.navigation

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class QuickAccess internal constructor(
    @DrawableRes val iconRes: Int,
    @StringRes private val titleRes: Int,
    private val customTitle: String?,
    val category: QuickAccessCategory,
    val isEnabled: Boolean
) {
    constructor(
        @DrawableRes iconRes: Int,
        @StringRes titleRes: Int,
        category: QuickAccessCategory,
        enabled: Boolean
    ) : this(iconRes, titleRes, null, category, enabled)

    val id: Long
        get() = category.hashCode().toLong()

    val key: String
        get() = category.toString()

    fun getTitle(context: Context): String =
        if (!customTitle.isNullOrEmpty()) customTitle else context.getString(titleRes)

    fun withSettings(settings: QuickAccessSettings): QuickAccess =
        QuickAccess(iconRes, titleRes, settings.customTitle, category, settings.isEnabled)

    fun toSettings(): QuickAccessSettings =
        QuickAccessSettings(category.toString(), customTitle, isEnabled)
}
