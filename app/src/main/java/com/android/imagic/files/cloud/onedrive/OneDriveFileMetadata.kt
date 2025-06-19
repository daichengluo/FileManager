package com.android.imagic.files.cloud.onedrive

import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.CloudFileType
import com.android.imagic.files.cloud.util.SpaceData
import com.android.imagic.files.file.asMimeType
import com.android.imagic.files.filelist.getCollationKeyForFileName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.Collator
import java.time.Instant

@Serializable
data class OneDriveFileMetadata(
    val id: String,
    val name: String,
    val size: Long?,
    val file: FileInfo? = null,
    val folder: FolderInfo? = null,
    val parentReference: ParentReference? = null,
    val createdDateTime: String?,
    val lastModifiedDateTime: String?,
    val thumbnails: List<ThumbnailSet>? = null
)

@Serializable
data class FileInfo(val mimeType: String?)

@Serializable
data class FolderInfo(val childCount: Int?)

@Serializable
data class ParentReference(val id: String?)

@Serializable
data class ThumbnailSet(val large: ThumbnailInfo?)

@Serializable
data class ThumbnailInfo(val url: String?)

fun OneDriveFileMetadata.toCloudFileItem(): CloudFileItem {
    val isDirectory = folder != null
    val nameCollationKey = Collator.getInstance().getCollationKeyForFileName(name)
    val mimeTypeString =
        file?.mimeType ?: if (isDirectory) "inode/directory" else "application/octet-stream"
    val mimeType = mimeTypeString.asMimeType()

    return CloudFileItem(
        id = id,
        parentId = parentReference?.id ?: "root",
        name = name,
        nameCollationKey = nameCollationKey,
        type = if (isDirectory) CloudFileType.DIRECTORY else CloudFileType.FILE,
        size = size ?: 0,
        url = null, // 若有分享 URL 可填入
        isHidden = false, // OneDrive 无直接隐藏字段
        mimeType = mimeType,
        thumbUrl = thumbnails?.firstOrNull()?.large?.url,
        createTime = createdDateTime?.let { Instant.parse(it) },
        modifyTime = lastModifiedDateTime?.let { Instant.parse(it) },
        childrenCount = folder?.childCount,
        fileHash = id // 可替换为 ETag 或自定义 hash
    )
}

@Serializable
data class OneDriveListResponse(
    val value: List<OneDriveFileMetadata>
)

@Serializable
data class OneDriveDownloadResponse(
    @SerialName("@microsoft.graph.downloadUrl")
    val downloadUrl: String
)

@Serializable
data class UploadSessionResponse(
    val uploadUrl: String,
    val expirationDateTime: String
)

@Serializable
data class StorageQuotaResponse(
    val id: String,
    val driveType: String,
    val quota: Quota
)

@Serializable
data class Quota(
    val remaining: Long,
    val state: String,
    val total: Long,
    val used: Long
)

fun Quota.toSpaceData(): SpaceData = SpaceData(
    total = total,
    used = used
)

@Serializable
data class TokenResponse(
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String
)
