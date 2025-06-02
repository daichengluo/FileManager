package com.android.imagic.files.file.cloud

import android.os.Parcelable
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.filelist.CloudPathData
import com.android.imagic.files.util.ParcelableParceler
import com.android.imagic.files.util.asFileName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import java.text.CollationKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Parcelize
data class CloudFileItem(
    val id: Int,
    val parentId: Int,
    val name: String,
    val nameCollationKey: @WriteWith<ParcelableParceler> CollationKey,
    val type: CloudFileType,
    val size: Long,
    val url: String? = null,
    val isHidden: Boolean,
    val mimeType: MimeType,
    val thumbUrl: String? = null,
    val createTime: String? = null,
    val modifyTime: String? = null,
    var childrenCount: Int? = null,
    val fileHash: String? = null
) : Parcelable {

    val isDirectory: Boolean
        get() = type == CloudFileType.DIRECTORY
}

enum class CloudFileType {
    DIRECTORY, FILE
}

val CloudFileItem.extension: String
    get() = if (isDirectory) "" else name.asFileName().extensions

val CloudFileItem.toCloudPathData: CloudPathData?
    get() = if (isDirectory) CloudPathData(id, name) else null

fun String.toInstant(): Instant = LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    .atZone(ZoneId.systemDefault())
    .toInstant()
