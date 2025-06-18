package com.android.imagic.files.cloud.gdrive

import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.CloudFileType
import com.android.imagic.files.cloud.util.SpaceData
import com.android.imagic.files.file.asMimeType
import com.android.imagic.files.filelist.getCollationKeyForFileName
import kotlinx.serialization.Serializable
import java.text.Collator
import java.time.Instant

@Serializable
data class GoogleDriveFileMetadata(
    val id: String,
    val name: String,
    val mimeType: String,
    val parents: List<String>?,
    val createdTime: String?,
    val modifiedTime: String?,
    val size: Long? = 0,
    val thumbnailLink: String? = null
)

fun GoogleDriveFileMetadata.toCloudFileItem(): CloudFileItem {
    val nameCollationKey = Collator.getInstance().getCollationKeyForFileName(name)
    val type = if (mimeType == "application/vnd.google-apps.folder") {
        CloudFileType.DIRECTORY
    } else CloudFileType.FILE
    val mimeType = mimeType.asMimeType()
    return CloudFileItem(
        id = id,
        parentId = parents?.last() ?: "root",
        name = name,
        nameCollationKey = nameCollationKey,
        type = type,
        size = size ?: 0L,
        url = null,
        isHidden = false,
        mimeType = mimeType,
        thumbUrl = thumbnailLink,
        createTime = Instant.parse(createdTime),
        modifyTime = Instant.parse(modifiedTime),
        fileHash = id,
        childrenCount = null
    )
}

@Serializable
data class FileOperationResponse(
    val kind: String,
    val id: String,
    val name: String,
    val mimeType: String
)

@Serializable
data class FileParentsResponse(
    val id: String,
    val parents: List<String>?
)

@Serializable
data class RenameFileResponse(
    val id: String,
    val name: String
)

@Serializable
data class GoogleDriveFileListResponse(
    val files: List<GoogleDriveFileMetadata>
)

@Serializable
data class StorageQuotaResponse(
    val storageQuota: StorageQuota
)

@Serializable
data class StorageQuota(
    val limit: Long,
    val usage: Long,
    val usageInDrive: Long,
    val usageInDriveTrash: Long
) {
    val remaining: Long
        get() = limit - usage
}

fun StorageQuota.toSpaceData(): SpaceData = SpaceData(
    total = limit,
    used = usage
)
