package com.android.imagic.files.filelist

import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.isSupportedArchive
import com.android.imagic.files.provider.archive.archiveFile
import com.android.imagic.files.provider.archive.isArchivePath
import com.android.imagic.files.provider.document.isDocumentPath
import com.android.imagic.files.provider.document.resolver.DocumentResolver
import com.android.imagic.files.provider.linux.isLinuxPath
import java8.nio.file.Path

val Path.name: String
    get() = fileName?.toString() ?: if (isArchivePath) archiveFile.fileName.toString() else "/"

fun Path.toUserFriendlyString(): String = if (isLinuxPath) toFile().path else toUri().toString()

fun Path.isArchiveFile(mimeType: MimeType): Boolean = !isArchivePath && mimeType.isSupportedArchive

val Path.isLocalPath: Boolean
    get() =
        isLinuxPath || (isDocumentPath && DocumentResolver.isLocal(this as DocumentResolver.Path))

val Path.isRemotePath: Boolean
    get() = !isLocalPath
