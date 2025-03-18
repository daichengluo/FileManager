package com.android.imagic.files.provider.common

import com.android.imagic.files.file.MimeTypeCategory
import java8.nio.file.Path
import java.io.IOException

interface Lister {
    @Throws(IOException::class)
    fun list(
        directory: Path,
        category: MimeTypeCategory?,
        intervalMillis: Long,
        listener: (List<Path>) -> Unit
    )
}
