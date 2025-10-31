package com.android.imagic.files.appwidget.receiver

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.imagic.files.appwidget.LoginWidgetUpdater

class LauncherLoginWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("chengluo", "LauncherLoginWidget onReceive: ")
        LoginWidgetUpdater.updateWidgets(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) = LoginWidgetUpdater.updateWidgets(context, appWidgetManager, appWidgetIds)
}