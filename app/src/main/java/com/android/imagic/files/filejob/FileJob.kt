package com.android.imagic.files.filejob

import android.content.Intent
import com.android.imagic.files.filejob.FileJobService.Actions
import com.android.imagic.files.filejob.FileJobService.Companion.FILE_TRANSFER_ID
import com.android.imagic.files.util.showToast
import java.io.IOException
import java.io.InterruptedIOException
import java.util.Random

abstract class FileJob {
    val id = Random().nextInt()

    internal lateinit var service: FileJobService
        private set

    fun runOn(service: FileJobService) {
        this.service = service
        try {
            run()
            // TODO: Toast
        } catch (e: InterruptedIOException) {
            // TODO
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
            service.showToast(e.toString())
        } finally {
            service.notificationManager.cancel(id)
            Intent(Actions.CANCELLED.toString()).apply {
                putExtra(FILE_TRANSFER_ID, id)
            }.also {
                service.localBroadcastManager.sendBroadcast(it)
            }
        }
    }

    @Throws(IOException::class)
    protected abstract fun run()
}
