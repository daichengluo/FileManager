package com.android.imagic.files.cloud.gdrive.sdk

import android.util.Log
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.CloudFileType
import com.android.imagic.files.cloud.DriveType
import com.android.imagic.files.cloud.util.SpaceData
import com.android.imagic.files.cloud.util.TransferStatus
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.asMimeType
import com.android.imagic.files.file.guessFromPath
import com.android.imagic.files.filelist.getCollationKeyForFileName
import com.android.imagic.files.provider.common.size
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.safeApiCall
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.FileContent
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java8.nio.file.Files
import java8.nio.file.Path
import java8.nio.file.StandardOpenOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.text.Collator
import java.time.Instant

fun File.toCloudFileItem(): CloudFileItem {
    val nameCollationKey = Collator.getInstance().getCollationKeyForFileName(name)
    val type = if (mimeType == "application/vnd.google-apps.folder") {
        CloudFileType.DIRECTORY
    } else CloudFileType.FILE
    val mimeType = mimeType.asMimeType()
    val fileSize = this.getSize() ?: 0L
    return CloudFileItem(
        id = id,
        parentId = parents?.last() ?: "root",
        name = name,
        nameCollationKey = nameCollationKey,
        type = type,
        size = fileSize,
        url = null,
        isHidden = false,
        mimeType = mimeType,
        thumbUrl = thumbnailLink,
        createTime = Instant.parse(createdTime?.toStringRfc3339()),
        modifyTime = Instant.parse(modifiedTime?.toStringRfc3339()),
        fileHash = id,
        childrenCount = null,
        driveType = DriveType.GOOGLE_DRIVE
    )
}

class GoogleDriveService(val drive: Drive) {

    suspend fun getFileList(
        directoryId: String,
        trashed: Boolean
    ): ApiOperation<List<CloudFileItem>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val query = "'$directoryId' in parents and trashed = $trashed"
            val fileList = drive.files().list()
                .setQ(query)
                .setFields("files(id,name,mimeType,parents,createdTime,modifiedTime,size,thumbnailLink)")
                .execute()

            fileList.files.map { file ->
                Log.d(
                    "chengluo",
                    "Google drive file: ${file.name} ${file.mimeType} ${file.size}"
                )

                file.toCloudFileItem()
            }
        }
    }

    suspend fun getChildrenCount(directoryId: String): Int = withContext(Dispatchers.IO) {
        var pageToken: String? = null
        var totalCount = 0

        do {
            val result = drive.files().list()
                .setQ("'$directoryId' in parents and trashed = false")
                .setFields("nextPageToken, files(id)")
                .setPageToken(pageToken)
                .execute()

            totalCount += result.files.size
            pageToken = result.nextPageToken
        } while (pageToken != null)

        totalCount
    }

    suspend fun createDirectory(directoryName: String, parentId: String): String? =
        withContext(Dispatchers.IO) {
            val metadata = File().apply {
                name = directoryName
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(parentId)
            }

            try {
                val result = drive.files()
                    .create(metadata)
                    .setFields("id, name")
                    .execute()
                result.id
            } catch (e: Exception) {
                Log.e("chengluo", "createDirectory: ${e.message}", e)
                null
            }
        }

    suspend fun createEmptyFile(filename: String, parentId: String): Boolean =
        withContext(Dispatchers.IO) {
            val fileMimeType = MimeType.guessFromPath(filename).value
            val metadata = File().apply {
                name = filename
                mimeType = fileMimeType
                parents = listOf(parentId)
            }

            val emptyContent = ByteArrayInputStream("".toByteArray(Charsets.UTF_8))
            val mediaContent = InputStreamContent(fileMimeType, emptyContent)

            try {
                drive.files()
                    .create(metadata, mediaContent)
                    .setFields("id, name")
                    .execute()
                true
            } catch (e: Exception) {
                Log.e("chengluo", "createEmptyFile: ${e.message}", e)
                false
            }
        }

    suspend fun uploadFile(file: java.io.File, parentId: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!file.exists()) file.createNewFile()
            val fileMimeType = MimeType.guessFromPath(file.path).value

            val metadata = File().apply {
                name = file.name
                mimeType = fileMimeType
                parents = listOf(parentId)
            }

            val mediaContent = FileContent(fileMimeType, file)

            try {
                drive.files()
                    .create(metadata, mediaContent)
                    .setFields("id, name")
                    .execute()
                true
            } catch (e: Exception) {
                Log.e("chengluo", "uploadFile: ${e.message}", e)
                false
            }
        }

    fun uploadFileWithProgressFlow(
        file: java.io.File,
        parentFolderId: String
    ): Flow<TransferStatus> = channelFlow {
        try {
            val metadata = File().apply {
                name = file.name
                parents = listOf(parentFolderId)
            }
            val fileMimeType = MimeType.guessFromPath(file.path).value
            val mediaContent = FileContent(fileMimeType, file)
            val createRequest = drive.files().create(metadata, mediaContent)
                .setFields("id, name")

            // 获取 uploader 设置监听器
            createRequest.mediaHttpUploader.apply {
                chunkSize = MediaHttpUploader.MINIMUM_CHUNK_SIZE
                isDirectUploadEnabled = false

                setProgressListener { progress ->
                    when (progress.uploadState) {
                        MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS -> {
                            trySend(
                                TransferStatus.Transferring(
                                    transferred = progress.numBytesUploaded,
                                    total = file.length()
                                )
                            ).isSuccess
                        }

                        MediaHttpUploader.UploadState.MEDIA_COMPLETE -> {
                            // will emit below
                        }

                        else -> Unit
                    }
                }
            }

            val uploadedFile = createRequest.execute()
            trySend(TransferStatus.Finished(uploadedFile.id))

        } catch (e: Exception) {
            trySend(TransferStatus.Failed(e))
        }
    }

    fun uploadPathWithProgressFlow(path: Path, parentFolderId: String): Flow<TransferStatus> =
        channelFlow {
            try {
                val file = path.toFile()
                val fileMimeType = MimeType.guessFromPath(path.toString()).value

                val metadata = File().apply {
                    name = file.name
                    parents = listOf(parentFolderId)
                }

                val mediaContent = FileContent(fileMimeType, file)
                val createRequest = drive.files().create(metadata, mediaContent)
                    .setFields("id, name")

                val uploader = createRequest.mediaHttpUploader.apply {
                    chunkSize = MediaHttpUploader.MINIMUM_CHUNK_SIZE
                    isDirectUploadEnabled = false

                    setProgressListener { progress ->
                        when (progress.uploadState) {
                            MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS -> {
                                trySend(
                                    TransferStatus.Transferring(
                                        transferred = progress.numBytesUploaded,
                                        total = file.length()
                                    )
                                )
                            }

                            MediaHttpUploader.UploadState.MEDIA_COMPLETE -> {
                                // do nothing here
                            }

                            else -> Unit
                        }
                    }
                }

                val uploaded = createRequest.execute()
                trySend(TransferStatus.Finished(uploaded.id))

            } catch (e: Exception) {
                trySend(TransferStatus.Failed(e))
            }
        }

    suspend fun renameFile(fileId: String, newName: String): Boolean =
        withContext(Dispatchers.IO) {
            val metadata = File().apply { name = newName }
            try {
                drive.files()
                    .update(fileId, metadata)
                    .setFields("id, name")
                    .execute()
                true
            } catch (e: Exception) {
                Log.e("chengluo", "renameFile: ${e.message}", e)
                false
            }
        }

    suspend fun deleteFile(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            drive.files().delete(fileId).execute()
            true
        } catch (e: Exception) {
            Log.e("chengluo", "deleteFile: ${e.message}", e)
            false
        }
    }

    suspend fun copyFile(
        fileId: String,
        targetFolderId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val metadata = File().apply { parents = listOf(targetFolderId) }
        try {
            drive.files().copy(fileId, metadata)
                .setFields("id, name, parents")
                .execute()
            true
        } catch (e: Exception) {
            Log.e("chengluo", "copyFile: ${e.message}", e)
            false
        }
    }

    suspend fun copyFileRecursively(
        sourceFolderId: String,
        targetParentFolderId: String
    ): Boolean = withContext(Dispatchers.IO) {
        // 1. 创建新的目标文件夹
        val copiedFolderMetadata = File().apply {
            name = drive.files().get(sourceFolderId).setFields("name").execute().name
            mimeType = "application/vnd.google-apps.folder"
            parents = listOf(targetParentFolderId)
        }
        val copiedFolder = drive.files().create(copiedFolderMetadata)
            .setFields("id")
            .execute()

        // 2. 查询原始文件夹中的所有文件与子文件夹
        val childFiles = mutableListOf<File>()
        var pageToken: String? = null
        do {
            val result = drive.files().list()
                .setQ("'$sourceFolderId' in parents and trashed = false")
                .setFields("nextPageToken, files(id, name, mimeType)")
                .setPageToken(pageToken)
                .execute()
            childFiles += result.files
            pageToken = result.nextPageToken
        } while (pageToken != null)

        // 3. 遍历处理子项
        for (child in childFiles) {
            if (child.mimeType == "application/vnd.google-apps.folder") {
                // 递归复制子文件夹
                copyFileRecursively(
                    sourceFolderId = child.id,
                    targetParentFolderId = copiedFolder.id
                )
            } else {
                // 复制文件到新文件夹
                val metadata = File().apply {
                    name = child.name
                    parents = listOf(copiedFolder.id)
                }
                drive.files().copy(child.id, metadata).execute()
            }
        }

        true
    }

    suspend fun moveFile(
        fileId: String,
        newParentId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = drive.files().get(fileId)
                .setFields("parents")
                .execute()
            val oldParents = file.parents?.joinToString(separator = ",")

            drive.files().update(fileId, null)
                .setAddParents(newParentId)
                .setRemoveParents(oldParents)
                .setFields("id, parents")
                .execute()
            true
        } catch (e: Exception) {
            Log.e("chengluo", "moveFile: ${e.message}", e)
            false
        }
    }

    fun downloadFile(fileId: String, targetPath: Path, fileSize: Long): Flow<TransferStatus> =
        channelFlow {
            launch(Dispatchers.IO) {
                try {
                    val outputStream = Files.newOutputStream(targetPath)
                    val request = drive.files().get(fileId).setAlt("media")

                    try {
                        val countingStream = object : OutputStream() {
                            var transferred = 0L
                            private var lastUpdateTime = 0L
                            private val updateInterval = 200L // 每 200ms 发送一次进度

                            override fun write(b: Int) {
                                outputStream.write(b)
                                transferred++
                                maybeSendProgress()
                            }

                            override fun write(b: ByteArray, off: Int, len: Int) {
                                outputStream.write(b, off, len)
                                transferred += len
                                maybeSendProgress()
                            }

                            override fun close() {
                                outputStream.close()
                            }

                            private fun maybeSendProgress() {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastUpdateTime > updateInterval) {
                                    trySend(TransferStatus.Transferring(transferred, fileSize))
                                    lastUpdateTime = currentTime
                                }
                            }
                        }

                        request.executeMediaAndDownloadTo(countingStream)
                        countingStream.close()

                        trySend(TransferStatus.Finished(targetPath.toString()))
                        close() // 关闭 channelFlow
                    } catch (e: Exception) {
                        trySend(TransferStatus.Failed(e))
                        close(e)
                    }
                } catch (e: Exception) {
                    trySend(TransferStatus.Failed(e))
                    close(e)
                }
            }
        }

    fun downloadFileWithResume(
        fileId: String,
        targetPath: Path,
        fileSize: Long
    ): Flow<TransferStatus> = channelFlow {
        launch(Dispatchers.IO) {
            // 当前已下载大小
            val downloadedBytes = targetPath.size()
            //val downloadedBytes = if (Files.exists(targetPath)) Files.size(targetPath) else 0L

            val outputStream = Files.newOutputStream(
                targetPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )

            // 创建自定义请求：设置 Range
            val requestFactory = NetHttpTransport().createRequestFactory { request ->
                val credential = drive.requestFactory.initializer
                credential?.initialize(request)
            }

            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = requestFactory.buildGetRequest(GenericUrl(url)).apply {
                if (downloadedBytes > 0) {
                    // 设置 Range 请求头
                    headers.range = "bytes=$downloadedBytes-"
                }
            }

            try {
                val response = request.execute()
                val inputStream = response.content

                val buffer = ByteArray(8 * 1024)
                var read: Int
                var transferred = downloadedBytes
                var lastUpdateTime = System.currentTimeMillis()
                val updateInterval = 200L

                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    transferred += read

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime >= updateInterval) {
                        trySend(TransferStatus.Transferring(transferred, fileSize))
                        lastUpdateTime = currentTime
                    }
                }
                // 补发最终进度
                trySend(TransferStatus.Transferring(transferred, fileSize))
                trySend(TransferStatus.Finished(targetPath.toString()))

            } catch (e: Exception) {
                trySend(TransferStatus.Failed(e))
                close(e)
            }
        }
    }

    suspend fun getSpaceData(): SpaceData = withContext(Dispatchers.IO) {
        val about = drive.about().get()
            .setFields("storageQuota")
            .execute()

        val storage = about.storageQuota
        SpaceData(
            total = storage.limit ?: 0L,
            used = storage.usage ?: 0L
        )
    }
}