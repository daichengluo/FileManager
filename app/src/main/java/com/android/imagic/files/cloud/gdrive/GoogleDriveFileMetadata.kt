package com.android.imagic.files.cloud.gdrive

import kotlinx.serialization.Serializable

@Serializable
data class GoogleDriveFileMetadata(
    val id: String,
    val name: String,
    val mimeType: String,
    val parents: List<String>?,
    val createdTime: String?,
    val modifiedTime: String?,
    val size: Long? = 0
)

@Serializable
data class CreateFolderResponse(
    val id: String,
    val kind: String,
    val name: String,
    val mimeType: String
)

@Serializable
data class GoogleDriveFileListResponse(
    val files: List<GoogleDriveFileMetadata>
)
