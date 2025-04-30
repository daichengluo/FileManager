package com.android.imagic.files.provider.cloud.sdrive.client

import kotlinx.serialization.Serializable

@Serializable
data class UploadFileInfo(
    val name: String?,
    val url: String?,
    val fileSizeInMB: Int
)

@Serializable
data class NewFolderBody(
    val newFolderName: String,
    val newParentID: Int
)

@Serializable
data class FileInfoBody(
    val ctFileName: String,
    val fileSize: Int,
    val filePath: String,
    val fileHash: String? = null,
    val targetID: Int
)

@Serializable
data class RenameBody(
    val id: Int,
    val newName: String
)

@Serializable
data class DirTreeBody(
    val id: Int
)

@Serializable
data class DeleteFilesBody(
    val arrayStr: String
)
