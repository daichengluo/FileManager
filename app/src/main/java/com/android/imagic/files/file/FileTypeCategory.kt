package com.android.imagic.files.file

import android.net.Uri
import android.provider.MediaStore

enum class FileTypeCategory {
    IMAGE, VIDEO, AUDIO, DOCUMENT, APK
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

fun FileTypeCategory.getQueryInfo(): Quadruple<Uri, Array<String>, String, Array<String>> {
    val projection = arrayOf(MediaStore.Files.FileColumns.DATA)

    return when (this) {
        FileTypeCategory.IMAGE -> Quadruple(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media.MIME_TYPE} LIKE ?",
            arrayOf("image/%")
        )

        FileTypeCategory.VIDEO -> Quadruple(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Video.Media.MIME_TYPE} LIKE ?",
            arrayOf("video/%")
        )

        FileTypeCategory.AUDIO -> Quadruple(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.MIME_TYPE} LIKE ?",
            arrayOf("audio/%")
        )

        FileTypeCategory.DOCUMENT -> Quadruple(
            MediaStore.Files.getContentUri("external"),
            projection,
            "${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                "text/plain",
                "application/pdf",
                "application/msword",
                "application/vnd.ms-excel",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            )
        )

        FileTypeCategory.APK -> Quadruple(
            MediaStore.Files.getContentUri("external"),
            projection,
            "${MediaStore.Files.FileColumns.MIME_TYPE} = ?",
            arrayOf("application/vnd.android.package-archive")
        )
    }
}
