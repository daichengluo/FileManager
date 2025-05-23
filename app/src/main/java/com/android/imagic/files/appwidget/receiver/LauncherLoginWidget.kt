package com.android.imagic.files.appwidget.receiver

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.android.imagic.files.appwidget.LoginWidgetUpdater

class LauncherLoginWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) = LoginWidgetUpdater.updateWidgets(context, appWidgetManager, appWidgetIds)
}