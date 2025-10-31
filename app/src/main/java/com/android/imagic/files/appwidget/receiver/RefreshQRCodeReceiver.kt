package com.android.imagic.files.appwidget.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.imagic.files.appwidget.LoginWidgetUpdater

class RefreshQRCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH_QRCODE) {
            Log.d("chengluo", "RefreshQRCodeReceiver onReceive: ")
            LoginWidgetUpdater.bitmap = null
            LoginWidgetUpdater.needRefresh = false
            LoginWidgetUpdater.updateWidgets(context)
        }
    }

    companion object {
        const val ACTION_REFRESH_QRCODE = "com.android.imagic.files.REFRESH_QRCODE"
    }
}