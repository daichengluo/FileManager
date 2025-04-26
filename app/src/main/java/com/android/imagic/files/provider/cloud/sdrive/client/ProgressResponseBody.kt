package com.android.imagic.files.provider.cloud.sdrive.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import okio.buffer
import okio.sink
import java.io.File

fun ResponseBody.saveFile(file: File, downloadedBytes: Long): Flow<TransferStatus> =
    flow {
        try {
            val totalBytes = contentLength() + downloadedBytes
            var progressBytes = downloadedBytes

            emit(TransferStatus.Transferring(((progressBytes * 100) / totalBytes).toInt()))

            source().use { source ->
                file.sink(append = true).buffer().use { sink ->
                    var bytesRead: Long
                    while ((source.read(sink.buffer, 8192).also { bytesRead = it }) != -1L) {
                        progressBytes += bytesRead
                        sink.emit()

                        emit(TransferStatus.Transferring(((progressBytes * 100) / totalBytes).toInt()))
                    }
                    sink.flush()
                }
            }
            emit(TransferStatus.Finished)
        } catch (e: Exception) {
            emit(TransferStatus.Failed(e))
        }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()
