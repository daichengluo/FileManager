package com.android.imagic.files.cloud.gdrive.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateFileRequest(
    val name: String,
    val mimeType: String,
    val parents: List<String>
)

@Serializable
data class CopyFileRequest(
    val name: String? = null,
    val parents: List<String>
)
