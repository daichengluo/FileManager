package com.android.imagic.files.provider.common

import java8.nio.file.attribute.FileTime
import java.time.Instant
import kotlin.reflect.KClass

val KClass<FileTime>.EPOCH: FileTime
    get() = FileTime.from(Instant.EPOCH)
