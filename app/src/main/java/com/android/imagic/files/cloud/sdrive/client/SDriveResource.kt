package com.android.imagic.files.cloud.sdrive.client

import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.util.TransferStatus
import java.io.File

interface SDriveResource {
    suspend fun getFileList(parentId: Int): List<CloudFileItem>
    suspend fun getFileSize(url: String): Long
    suspend fun download(url: String, file: File, callback: ((TransferStatus) -> Unit)? = null)
    suspend fun makeDirectory(name: String, parentId: Int): Boolean
    suspend fun upload(file: File, parentId: Int, callback: ((TransferStatus) -> Unit)? = null)
    suspend fun delete(fileId: Int): Boolean
    suspend fun copy(sourceId: Int, parentId: Int, callback: ((TransferStatus) -> Unit)? = null)
    suspend fun move(
        sourceId: Int,
        targetParentId: Int,
        callback: ((TransferStatus) -> Unit)? = null
    )

    suspend fun rename(newName: String, fileId: Int): Boolean
}