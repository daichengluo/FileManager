package com.android.imagic.files.navigation

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class QuickAccess(
    @DrawableRes val iconRes: Int,
    @StringRes private val titleRes: Int,
    private val customTitle: String? = null,
    val category: QuickAccessCategory,
    val relativePath: String,
    val isEnabled: Boolean = true
) {
    val id: Long
        get() = relativePath.hashCode().toLong()

    val key: String
        get() = relativePath

    fun getTitle(context: Context): String =
        if (!customTitle.isNullOrEmpty()) customTitle else context.getString(titleRes)

    fun withSettings(settings: QuickAccessSettings): QuickAccess =
        QuickAccess(
            iconRes,
            titleRes,
            settings.customTitle,
            settings.category,
            relativePath,
            settings.isEnabled
        )

    fun toSettings(): QuickAccessSettings =
        QuickAccessSettings(relativePath, customTitle, category, isEnabled)

    companion object {
        const val QUICK_ACCESS_APK = "APK"
    }
}
