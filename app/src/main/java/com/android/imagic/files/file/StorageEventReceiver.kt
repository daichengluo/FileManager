package com.android.imagic.files.file

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class StorageEventReceiver(private val onStorageChanged: () -> Unit) {
    private var isRegistered = false
    private var receiver: BroadcastReceiver? = null

    fun register(context: Context) {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            addDataScheme("file")
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_MEDIA_MOUNTED,
                    Intent.ACTION_MEDIA_UNMOUNTED,
                    Intent.ACTION_MEDIA_REMOVED,
                    Intent.ACTION_MEDIA_BAD_REMOVAL -> {
                        onStorageChanged()
                    }
                }
            }
        }
        context.registerReceiver(receiver, filter)
        isRegistered = true
    }

    fun unregister(context: Context) {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        isRegistered = false
        receiver = null
    }
}
