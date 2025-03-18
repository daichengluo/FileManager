package com.android.imagic.files.provider.common

import com.android.imagic.files.file.MimeTypeCategory
import java8.nio.file.FileVisitResult
import java8.nio.file.FileVisitor
import java8.nio.file.Files
import java8.nio.file.Path
import java8.nio.file.attribute.BasicFileAttributes
import java.io.IOException
import java.io.InterruptedIOException

object WalkFileTreeLister {
    @Throws(IOException::class)
    fun list(
        directory: Path,
        category: MimeTypeCategory?,
        intervalMillis: Long,
        listener: (List<Path>) -> Unit
    ) {
        val paths = mutableListOf<Path>()
        var lastProgressMillis = System.currentTimeMillis()

        walkFileTreeForList(directory, object : FileVisitor<Path> {
            @Throws(InterruptedIOException::class)
            override fun preVisitDirectory(
                directory: Path,
                attributes: BasicFileAttributes
            ): FileVisitResult {
                return FileVisitResult.CONTINUE
            }

            @Throws(InterruptedIOException::class)
            override fun visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult {
                if (matchesCategory(file, category)) {
                    paths.add(file)
                }

                val currentTimeMillis = System.currentTimeMillis()
                if (currentTimeMillis >= lastProgressMillis + intervalMillis) {
                    listener(paths)
                    paths.clear()
                    lastProgressMillis = currentTimeMillis
                }

                return FileVisitResult.CONTINUE
            }

            @Throws(InterruptedIOException::class)
            override fun visitFileFailed(file: Path, exception: IOException): FileVisitResult {
                exception.printStackTrace()
                return FileVisitResult.CONTINUE
            }

            @Throws(InterruptedIOException::class)
            override fun postVisitDirectory(
                directory: Path,
                exception: IOException?
            ): FileVisitResult {
                exception?.printStackTrace()
                return FileVisitResult.CONTINUE
            }
        })

        if (paths.isNotEmpty()) {
            listener(paths)
        }
    }

    private fun matchesCategory(file: Path, category: MimeTypeCategory?): Boolean {
        if (category == null) return true

        val mimeType = Files.probeContentType(file) ?: return false
        val type = mimeType.substringBefore("/")
        val subtype = mimeType.substringAfter("/", "")

        return when (category) {
            MimeTypeCategory.IMAGE -> type == "image"
            MimeTypeCategory.VIDEO -> type == "video"
            MimeTypeCategory.AUDIO -> type == "audio"
            MimeTypeCategory.DOCUMENT -> type == "text" || subtype in setOf(
                "pdf", "msword", "vnd.ms-excel",
                "vnd.openxmlformats-officedocument.wordprocessingml.document",
                "vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )

            MimeTypeCategory.APK -> mimeType == "application/vnd.android.package-archive"
            MimeTypeCategory.OTHER -> true
        }
    }

    @Throws(IOException::class)
    private fun walkFileTreeForList(start: Path, visitor: FileVisitor<in Path>) {
        Files.walkFileTree(start, visitor)
    }
}
