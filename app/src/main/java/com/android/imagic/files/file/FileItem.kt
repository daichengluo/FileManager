package com.android.imagic.files.file

import android.os.Parcelable
import androidx.annotation.WorkerThread
import com.android.imagic.files.filelist.getCollationKeyForFileName
import com.android.imagic.files.filelist.name
import com.android.imagic.files.provider.common.AndroidFileTypeDetector
import com.android.imagic.files.provider.common.isHidden
import com.android.imagic.files.provider.common.newDirectoryStream
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
    val mimeType: MimeType,
    var totalChildrenCount: Int? = null,
    var visibleChildrenCount: Int? = null
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
    var totalChildrenCount: Int? = null
    var visibleChildrenCount: Int? = null

    if (attributes.isDirectory) {
        try {
            newDirectoryStream().use { stream ->
                val counts = stream.fold(0 to 0) { (total, visible), path ->
                    val hidden = path.isHidden
                    (total + 1) to (if (!hidden) visible + 1 else visible)
                }
                totalChildrenCount = counts.first
                visibleChildrenCount = counts.second
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    if (!attributes.isSymbolicLink) {
        val mimeType = AndroidFileTypeDetector.getMimeType(this, attributes).asMimeType()
        return FileItem(
            this, nameCollationKey, attributes, null, null, isHidden, mimeType,
            totalChildrenCount, visibleChildrenCount
        )
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
        isHidden, mimeType, totalChildrenCount, visibleChildrenCount
    )
}
