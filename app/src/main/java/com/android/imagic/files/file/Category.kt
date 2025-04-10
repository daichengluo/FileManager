package com.android.imagic.files.file

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.android.imagic.files.navigation.QuickAccess

data class CategoryWrap(val context: Context, val quickAccess: QuickAccess?)

enum class Category {
    IMAGE, VIDEO, AUDIO, DOCUMENT, APK
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

fun Category.getQueryInfo(): Quadruple<Uri, Array<String>, String, Array<String>> {
    val projection = arrayOf(MediaStore.Files.FileColumns.DATA)

    return when (this) {
        Category.IMAGE -> Quadruple(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media.MIME_TYPE} LIKE ?",
            arrayOf("image/%")
        )

        Category.VIDEO -> Quadruple(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Video.Media.MIME_TYPE} LIKE ?",
            arrayOf("video/%")
        )

        Category.AUDIO -> Quadruple(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.MIME_TYPE} LIKE ?",
            arrayOf("audio/%")
        )

        Category.DOCUMENT -> Quadruple(
            MediaStore.Files.getContentUri("external"),
            projection,
            "${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?, ?)",
            arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain"
            )
        )

        Category.APK -> Quadruple(
            MediaStore.Files.getContentUri("external"),
            projection,
            "${MediaStore.Files.FileColumns.MIME_TYPE} = ?",
            arrayOf("application/vnd.android.package-archive")
        )
    }
}
