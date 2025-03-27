package com.android.imagic.files.navigation

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.imagic.files.file.MimeTypeCategory

data class QuickAccess internal constructor(
    @DrawableRes val iconRes: Int,
    @StringRes private val titleRes: Int,
    private val customTitle: String?,
    val category: MimeTypeCategory?,
    val relativePath: String,
    val isEnabled: Boolean
) {
    constructor(
        @DrawableRes iconRes: Int,
        @StringRes titleRes: Int,
        category: MimeTypeCategory,
        relativePath: String,
        enabled: Boolean
    ) : this(iconRes, titleRes, null, category, relativePath, enabled)

    constructor(
        @DrawableRes iconRes: Int,
        @StringRes titleRes: Int,
        relativePath: String,
        enabled: Boolean
    ) : this(iconRes, titleRes, null, null, relativePath, enabled)

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
