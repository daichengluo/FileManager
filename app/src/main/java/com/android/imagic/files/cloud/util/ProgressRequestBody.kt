package com.android.imagic.files.cloud.util

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File

class ProgressRequestBody(
    private val file: File,
    private val contentType: MediaType,
    private val onProgress: (transferred: Long, total: Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType = contentType

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val length = contentLength()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploaded = 0L
        file.inputStream().use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                onProgress(uploaded, length)
            }
        }
    }
}
