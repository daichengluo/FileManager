package com.android.imagic.files.provider.content

import android.net.Uri
import android.os.Parcelable
import com.android.imagic.files.provider.common.AbstractContentProviderFileAttributes
import com.android.imagic.files.provider.common.EPOCH
import com.android.imagic.files.provider.common.FileTimeParceler
import java8.nio.file.attribute.FileTime
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

@Parcelize
internal class ContentFileAttributes(
    override val lastModifiedTime: @WriteWith<FileTimeParceler> FileTime,
    override val mimeType: String?,
    override val size: Long,
    override val fileKey: Parcelable
) : AbstractContentProviderFileAttributes() {
    companion object {
        fun from(mimeType: String?, size: Long, uri: Uri): ContentFileAttributes {
            val lastModifiedTime = FileTime::class.EPOCH
            val fileKey = uri
            return ContentFileAttributes(lastModifiedTime, mimeType, size, fileKey)
        }
    }
}
