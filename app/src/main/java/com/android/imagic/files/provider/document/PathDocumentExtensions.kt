package com.android.imagic.files.provider.document

import android.net.Uri
import com.android.imagic.files.provider.content.resolver.ResolverException
import com.android.imagic.files.provider.document.resolver.DocumentResolver
import java8.nio.file.Path
import java8.nio.file.ProviderMismatchException
import java.io.IOException

val Path.documentUri: Uri
    @Throws(IOException::class)
    get() {
        this as? DocumentPath ?: throw ProviderMismatchException(toString())
        return try {
            DocumentResolver.getDocumentUri(this)
        } catch (e: ResolverException) {
            throw e.toFileSystemException(toString())
        }
    }

val Path.documentTreeUri: Uri
    get() {
        this as? DocumentPath ?: throw ProviderMismatchException(toString())
        return treeUri
    }

fun Uri.createDocumentTreeRootPath(): Path =
    DocumentFileSystemProvider.getOrNewFileSystem(this).rootDirectory
