package com.android.imagic.files.cloud.sdrive.client

import kotlinx.serialization.Serializable

@Serializable
data class UploadFileInfo(
    val name: String?,
    val url: String?
)

@Serializable
data class NewFolderBody(
    val newFolderName: String,
    val newParentID: String
)

@Serializable
data class FileInfoBody(
    val ctFileName: String,
    val fileSize: Long,
    val filePath: String,
    val fileHash: String? = null,
    val targetID: String
)

@Serializable
data class RenameBody(
    val id: String,
    val newName: String
)

@Serializable
data class DirTreeBody(
    val id: String
)

@Serializable
data class DeleteFilesBody(
    val arrayStr: String
)
