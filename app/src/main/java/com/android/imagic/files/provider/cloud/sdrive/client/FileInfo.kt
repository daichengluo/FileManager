package com.android.imagic.files.provider.cloud.sdrive.client

import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.asMimeType
import com.android.imagic.files.file.cloud.sdrive.CloudFileItem
import com.android.imagic.files.file.cloud.sdrive.CloudFileType
import com.android.imagic.files.file.guessFromPath
import com.android.imagic.files.file.isImage
import com.android.imagic.files.filelist.getCollationKeyForFileName
import kotlinx.serialization.Serializable
import java.text.Collator

@Serializable
data class FileInfo(
    val id: Int,
    val fName: String? = null,
    val fType: Int,
    val parentID: Int,
    val thumbURL: String? = null,
    val groupID: String? = null,
    val createUser: String? = null,
    val createTime: String? = null,
    val modifyTime: String? = null,
    val fileSize: Double? = null,
    val filePath: String? = null,
    val expand5: String? = null,
    val state: Int,
    val fileHash: String? = null
)

fun FileInfo.toCloudFileItem(): CloudFileItem {
    val name = fName ?: ""
    val nameCollationKey = Collator.getInstance().getCollationKeyForFileName(name)
    val type = if (fType == 1) CloudFileType.FILE else CloudFileType.DIRECTORY
    val size = fileSize?.toLong() ?: 0L
    val mimeType = MimeType.guessFromPath(name).value.asMimeType()
    val thumbUrl = if (mimeType.isImage && thumbURL == null) filePath else thumbURL
    val modifyTime = modifyTime ?: createTime
    return CloudFileItem(
        id = id,
        parentId = parentID,
        name = name,
        nameCollationKey = nameCollationKey,
        type = type,
        size = size,
        url = filePath,
        isHidden = false,
        mimeType = mimeType,
        thumbUrl = thumbUrl,
        createTime = createTime,
        modifyTime = modifyTime,
        fileHash = fileHash,
        childrenCount = expand5?.toInt()
    )
}

fun FileInfo.toFileInfoBody() = FileInfoBody(
    ctFileName = fName ?: "",
    fileSize = fileSize?.div(1024 * 1024)?.toInt() ?: 0,
    filePath = filePath ?: "",
    fileHash = fileHash,
    targetID = parentID
)
