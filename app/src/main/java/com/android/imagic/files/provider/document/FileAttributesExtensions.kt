package com.android.imagic.files.provider.document

import android.provider.DocumentsContract
import com.android.imagic.files.util.hasBits
import java8.nio.file.ProviderMismatchException
import java8.nio.file.attribute.BasicFileAttributes

val BasicFileAttributes.documentSupportsThumbnail: Boolean
    get() {
        this as? DocumentFileAttributes ?: throw ProviderMismatchException(toString())
        return flags().hasBits(DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL)
    }
