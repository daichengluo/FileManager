package com.android.imagic.files.cloud.util

import java8.nio.file.Files
import java8.nio.file.Path
import java8.nio.file.StandardOpenOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import okio.buffer
import okio.sink

fun ResponseBody.saveFile(
    path: Path,
    downloadedBytes: Long,
    totalBytes: Long
): Flow<TransferStatus> = flow {
    if (totalBytes > 0 && downloadedBytes >= totalBytes) {
        emit(TransferStatus.Finished())
        return@flow
    }

    try {
        var progressBytes = downloadedBytes
        var lastEmitBytes = downloadedBytes
        val emitInterval = 2048 * 1024L

        emit(TransferStatus.Transferring(progressBytes, totalBytes))

        source().use { source ->
            val outputStream = Files.newOutputStream(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
            )
            outputStream.sink().buffer().use { sink ->
                var bytesRead: Long
                while ((source.read(sink.buffer, 4096).also { bytesRead = it }) != -1L) {
                    progressBytes += bytesRead
                    sink.emit()

                    if (progressBytes - lastEmitBytes >= emitInterval || progressBytes == totalBytes) {
                        emit(TransferStatus.Transferring(progressBytes, totalBytes))
                        lastEmitBytes = progressBytes
                    }
                }
                sink.flush()
            }
        }
        emit(TransferStatus.Finished())
    } catch (e: Exception) {
        emit(TransferStatus.Failed(e))
    }
}.flowOn(Dispatchers.IO).distinctUntilChanged()
