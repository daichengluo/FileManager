package com.android.imagic.files.file

import android.content.Context
import com.android.imagic.files.navigation.QuickAccess
import com.android.imagic.files.navigation.QuickAccessCategory

data class QuickAccessWrap(
    val quickAccess: QuickAccess,
    val context: Context
) {
    val title: String
        get() = quickAccess.getTitle(context)

    val category: FileTypeCategory?
        get() = when (quickAccess.category) {
            QuickAccessCategory.IMAGE -> FileTypeCategory.IMAGE
            QuickAccessCategory.VIDEO -> FileTypeCategory.VIDEO
            QuickAccessCategory.AUDIO -> FileTypeCategory.AUDIO
            QuickAccessCategory.DOCUMENT -> FileTypeCategory.DOCUMENT
            QuickAccessCategory.APK -> FileTypeCategory.APK
            QuickAccessCategory.DOWNLOAD -> null
        }
}
