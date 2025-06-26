package com.android.imagic.files.cloud

import android.os.Parcelable
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.filelist.CloudPathData
import com.android.imagic.files.util.ParcelableParceler
import com.android.imagic.files.util.asFileName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import java.text.CollationKey
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Parcelize
data class CloudFileItem(
    val id: String,
    val parentId: String,
    val name: String,
    val nameCollationKey: @WriteWith<ParcelableParceler> CollationKey,
    val type: CloudFileType,
    val size: Long,
    val url: String? = null,
    val isHidden: Boolean,
    val mimeType: MimeType,
    val thumbUrl: String? = null,
    val createTime: Instant? = null,
    val modifyTime: Instant? = null,
    var childrenCount: Int? = null,
    val fileHash: String? = null,
    val driveType: DriveType
) : Parcelable {

    val isDirectory: Boolean
        get() = type == CloudFileType.DIRECTORY

    val isStarBoardDrive: Boolean
        get() = driveType == DriveType.STAR_BOARD

    val isGoogleDrive: Boolean
        get() = driveType == DriveType.GOOGLE_DRIVE

    val isOneDrive: Boolean
        get() = driveType == DriveType.ONEDRIVE
}

enum class CloudFileType {
    DIRECTORY, FILE
}

enum class DriveType {
    STAR_BOARD, ONEDRIVE, GOOGLE_DRIVE
}

val CloudFileItem.extension: String
    get() = if (isDirectory) "" else name.asFileName().extensions

val CloudFileItem.toCloudPathData: CloudPathData?
    get() = if (isDirectory) CloudPathData(id, name) else null

fun String.toInstant(): Instant {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'GMT'XXX", Locale.ENGLISH)
    return OffsetDateTime.parse(this, formatter).toInstant()
}
