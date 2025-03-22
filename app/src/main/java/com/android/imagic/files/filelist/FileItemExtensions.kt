package com.android.imagic.files.filelist

import android.content.Context
import android.os.Build
import com.android.imagic.files.file.FileItem
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.getBrokenSymbolicLinkName
import com.android.imagic.files.file.getName
import com.android.imagic.files.file.isApk
import com.android.imagic.files.file.isImage
import com.android.imagic.files.file.isMedia
import com.android.imagic.files.file.isPdf
import com.android.imagic.files.provider.archive.createArchiveRootPath
import com.android.imagic.files.provider.common.isHidden
import com.android.imagic.files.provider.document.documentSupportsThumbnail
import com.android.imagic.files.provider.document.isDocumentPath
import com.android.imagic.files.provider.ftp.isFtpPath
import com.android.imagic.files.provider.linux.isLinuxPath
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.util.asFileName
import com.android.imagic.files.util.isGetPackageArchiveInfoCompatible
import com.android.imagic.files.util.isMediaMetadataRetrieverCompatible
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Files
import java8.nio.file.Path
import java8.nio.file.attribute.BasicFileAttributes
import java8.nio.file.attribute.FileTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.CollationKey

val FileItem.name: String
    get() = path.name

val FileItem.baseName: String
    get() = if (attributes.isDirectory) name else name.asFileName().baseName

val FileItem.extension: String
    get() = if (attributes.isDirectory) "" else name.asFileName().extensions

fun FileItem.getMimeTypeName(context: Context): String {
    if (attributesNoFollowLinks.isSymbolicLink && isSymbolicLinkBroken) {
        return MimeType.getBrokenSymbolicLinkName(context)
    }
    return mimeType.getName(extension, context)
}

val FileItem.isArchiveFile: Boolean
    get() = path.isArchiveFile(mimeType)

val FileItem.isListable: Boolean
    get() = attributes.isDirectory || isArchiveFile

val FileItem.listablePath: Path
    get() = if (isArchiveFile) path.createArchiveRootPath() else path

// @see PathAttributesFetcher.fetch
val FileItem.supportsThumbnail: Boolean
    get() {
        if (path.isDocumentPath && attributes.documentSupportsThumbnail) {
            return true
        }
        if (path.isRemotePath) {
            val shouldReadRemotePath = !path.isFtpPath
                    && Settings.READ_REMOTE_FILES_FOR_THUMBNAIL.valueCompat
            if (!shouldReadRemotePath) {
                return false
            }
        }
        return when {
            mimeType.isApk && path.isGetPackageArchiveInfoCompatible -> true
            mimeType.isImage -> true
            mimeType.isMedia && path.isMediaMetadataRetrieverCompatible -> true
            mimeType.isPdf && (path.isLinuxPath || path.isDocumentPath) ->
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                        || Settings.SHOW_PDF_THUMBNAIL_PRE_28.valueCompat

            else -> false
        }
    }

// @see android.content.pm.parsing.ApkLiteParseUtils.parsePackageSplitNames
// @see android.content.pm.parsing.ParsingPackageUtils.validateName
// @see com.android.server.pm.PackageManagerService.getNextCodePath
private const val PACKAGE_NAME_COMPONENT_PATTERN = "[A-Za-z][0-9A-Z_a-z]*"
private const val PACKAGE_NAME_PATTERN =
    "$PACKAGE_NAME_COMPONENT_PATTERN(?:\\.$PACKAGE_NAME_COMPONENT_PATTERN)+"
private const val BASE64_URL_SAFE_CHARACTER_CLASS = "[0-9A-Za-z\\-_]"
private const val BASE64_URL_SAFE_PATTERN = ("(?:$BASE64_URL_SAFE_CHARACTER_CLASS{4})*"
        + "(?:$BASE64_URL_SAFE_CHARACTER_CLASS{3}=|$BASE64_URL_SAFE_CHARACTER_CLASS{2}==)?")
private val APP_DIRECTORY_REGEX =
    Regex("($PACKAGE_NAME_PATTERN)(?:-$BASE64_URL_SAFE_PATTERN)?")

val FileItem.appDirectoryPackageName: String?
    get() {
        if (!attributes.isDirectory) {
            return null
        }
        return APP_DIRECTORY_REGEX.matchEntire(name)?.groupValues?.get(1)
    }

fun FileItem.createDummyArchiveRoot(): FileItem =
    FileItem(
        path.createArchiveRootPath(), DummyCollationKey(), DummyArchiveRootBasicFileAttributes(),
        null, null, false, MimeType.DIRECTORY
    )

suspend fun FileItem.getFolderItemCount(
    isShowHiddenFiles: Boolean,
    listener: (Int) -> Unit
) = withContext(Dispatchers.IO) {
    if (!Files.isDirectory(path)) {
        withContext(Dispatchers.Main) {
            listener(0)
        }
    }
    val count = try {
        Files.list(path).filter { isShowHiddenFiles || !it.isHidden }.count().toInt()
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
    withContext(Dispatchers.Main) {
        listener(count)
    }
}

// Dummy collation key only to be added to the selection set, which may be used to determine file
// type when confirming deletion.
private class DummyCollationKey : CollationKey("") {
    override fun compareTo(other: CollationKey?): Int {
        throw UnsupportedOperationException()
    }

    override fun toByteArray(): ByteArray {
        throw UnsupportedOperationException()
    }
}

// Dummy attributes only to be added to the selection set, which may be used to determine file
// type when confirming deletion.
private class DummyArchiveRootBasicFileAttributes : BasicFileAttributes {
    override fun lastModifiedTime(): FileTime {
        throw UnsupportedOperationException()
    }

    override fun lastAccessTime(): FileTime {
        throw UnsupportedOperationException()
    }

    override fun creationTime(): FileTime {
        throw UnsupportedOperationException()
    }

    override fun isRegularFile(): Boolean = false

    override fun isDirectory(): Boolean = true

    override fun isSymbolicLink(): Boolean = false

    override fun isOther(): Boolean = false

    override fun size(): Long {
        throw UnsupportedOperationException()
    }

    override fun fileKey(): Any {
        throw UnsupportedOperationException()
    }
}
