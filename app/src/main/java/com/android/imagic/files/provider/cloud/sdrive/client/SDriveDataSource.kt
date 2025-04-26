package com.android.imagic.files.provider.cloud.sdrive.client

import com.android.imagic.files.file.cloud.sdrive.CloudFileItem
import com.android.imagic.files.util.ApiOperation
import kotlinx.coroutines.flow.Flow
import java.io.File

interface SDriveDataSource {
    suspend fun getContentLength(url: String): ApiOperation<Long>
    suspend fun downloadFile(url: String, file: File): Flow<TransferStatus>
    suspend fun getFileList(directoryId: Int): ApiOperation<List<CloudFileItem>>
    suspend fun createDirectory(name: String, parentId: Int): ApiOperation<Boolean>
    suspend fun uploadFile(userId: Int, file: File)
    suspend fun uploadFileInfo(body: FileInfoBody): ApiOperation<Boolean>
    suspend fun renameFile(newName: String, fileId: Int): ApiOperation<Boolean>
    suspend fun getDirTree(directoryId: Int): ApiOperation<List<CloudFileItem>>
    suspend fun deleteFiles(fileIds: String): ApiOperation<Boolean>
    suspend fun moveFiles(fileIds: String, targetId: Int): ApiOperation<Boolean>
    suspend fun copyFiles(fileIds: String, targetId: Int): ApiOperation<Boolean>
}