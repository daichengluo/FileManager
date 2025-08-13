package com.android.imagic.files.cloud.onedrive

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateFileRequest(
    val name: String,
    val folder: Map<String, String>,
    @SerialName("@microsoft.graph.conflictBehavior")
    val conflictBehavior: String
)

@Serializable
data class UploadFileRequestItem(
    val item: UploadFileRequest
) {
    @Serializable
    data class UploadFileRequest(
        @SerialName("@microsoft.graph.conflictBehavior")
        val conflictBehavior: String
    )
}

@Serializable
data class OperationFileRequest(
    val parentReference: ParentReference
)
