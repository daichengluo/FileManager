package com.android.imagic.files.appwidget.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.android.imagic.files.R
import com.android.imagic.files.appwidget.QrCodeLoginActivity
import com.android.imagic.files.settings.SettingsActivity

class LauncherLoginWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->

            val intent = Intent(context, QrCodeLoginActivity::class.java)
            val intent2 = Intent(context, SettingsActivity::class.java)

            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val pendingIntent2 = PendingIntent.getActivity(
                context, 0, intent2,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_launcher_login).apply {
                setOnClickPendingIntent(R.id.loginButton, pendingIntent)
                setOnClickPendingIntent(R.id.loginActonButton, pendingIntent2)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}