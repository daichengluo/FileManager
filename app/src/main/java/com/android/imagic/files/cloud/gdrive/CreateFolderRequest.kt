package com.android.imagic.files.cloud.gdrive

import kotlinx.serialization.Serializable

@Serializable
data class CreateFolderRequest(
    val name: String,
    val mimeType: String,
    val parents: List<String>
)
