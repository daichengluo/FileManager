package com.android.imagic.files.file

import java8.nio.file.attribute.BasicFileAttributes
import java.time.Instant

val BasicFileAttributes.fileSize: FileSize
    get() = size().asFileSize()

val BasicFileAttributes.lastModifiedInstant: Instant
    get() = lastModifiedTime().toInstant()
