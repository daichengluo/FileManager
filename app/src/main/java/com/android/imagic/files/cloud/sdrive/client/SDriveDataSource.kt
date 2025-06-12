package com.android.imagic.files.cloud.sdrive.client

import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.util.TransferStatus
import com.android.imagic.files.util.ApiOperation
import java8.nio.file.Path
import kotlinx.coroutines.flow.Flow
import java.io.File

interface SDriveDataSource {
    suspend fun getContentLength(url: String): ApiOperation<Long>
    suspend fun downloadFile(url: String, path: Path): Flow<TransferStatus>
    suspend fun getFileList(directoryId: String): ApiOperation<List<CloudFileItem>>
    suspend fun createDirectory(name: String, parentId: String): ApiOperation<CloudFileItem>
    suspend fun uploadFile(userId: Int, file: File): ApiOperation<UploadFileInfo>
    suspend fun uploadFile(userId: Int, path: Path): ApiOperation<UploadFileInfo>
    suspend fun uploadChunks(userId: Int, path: Path, chunkSize: Long): Flow<TransferStatus>
    suspend fun uploadFileInfo(
        name: String,
        size: Long,
        url: String,
        targetId: String
    ): ApiOperation<Boolean>

    suspend fun renameFile(newName: String, fileId: String): ApiOperation<Boolean>
    suspend fun getDirTree(directoryId: String): ApiOperation<List<CloudFileItem>>
    suspend fun deleteFiles(fileIds: String): ApiOperation<Boolean>
    suspend fun moveFiles(fileIds: String, targetId: String): ApiOperation<Boolean>
    suspend fun copyFiles(fileIds: String, targetId: String): ApiOperation<Boolean>
}