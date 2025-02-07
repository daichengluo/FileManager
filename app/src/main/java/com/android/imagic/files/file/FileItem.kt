package com.android.imagic.files.file

import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.android.imagic.files.filelist.getCollationKeyForFileName
import com.android.imagic.files.filelist.name
import com.android.imagic.files.provider.common.AndroidFileTypeDetector
import com.android.imagic.files.provider.common.isHidden
import com.android.imagic.files.provider.common.readAttributes
import com.android.imagic.files.provider.common.readSymbolicLinkByteString
import com.android.imagic.files.util.ParcelableParceler
import java8.nio.file.LinkOption
import java8.nio.file.Path
import java8.nio.file.attribute.BasicFileAttributes
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import java.io.IOException
import java.text.CollationKey
import java.text.Collator

@Parcelize
data class FileItem(
    val path: @WriteWith<ParcelableParceler> Path,
    val nameCollationKey: @WriteWith<ParcelableParceler> CollationKey,
    val attributesNoFollowLinks: @WriteWith<ParcelableParceler> BasicFileAttributes,
    val symbolicLinkTarget: String?,
    private val symbolicLinkTargetAttributes: @WriteWith<ParcelableParceler> BasicFileAttributes?,
    val isHidden: Boolean,
    val mimeType: MimeType
) : Parcelable {
    val attributes: BasicFileAttributes
        get() = symbolicLinkTargetAttributes ?: attributesNoFollowLinks

    val isSymbolicLinkBroken: Boolean
        get() {
            check(attributesNoFollowLinks.isSymbolicLink) { "Not a symbolic link" }
            return symbolicLinkTargetAttributes == null
        }
}

@WorkerThread
@Throws(IOException::class)
fun Path.loadFileItem(): FileItem {
    val nameCollationKey = Collator.getInstance().getCollationKeyForFileName(name)
    val attributes = readAttributes(BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
    val isHidden = isHidden
    if (!attributes.isSymbolicLink) {
        val mimeType = AndroidFileTypeDetector.getMimeType(this, attributes).asMimeType()
        return FileItem(this, nameCollationKey, attributes, null, null, isHidden, mimeType)
    }
    val symbolicLinkTarget = readSymbolicLinkByteString().toString()
    val symbolicLinkTargetAttributes = try {
        readAttributes(BasicFileAttributes::class.java)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
    val mimeType = AndroidFileTypeDetector.getMimeType(
        this, symbolicLinkTargetAttributes ?: attributes
    ).asMimeType()
    return FileItem(
        this, nameCollationKey, attributes, symbolicLinkTarget, symbolicLinkTargetAttributes,
        isHidden, mimeType
    )
}
