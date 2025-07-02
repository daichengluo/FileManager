package com.android.imagic.files.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import com.android.imagic.files.R
import com.android.imagic.files.appwidget.receiver.LauncherLoginWidget
import com.android.imagic.files.file.asFileSize
import com.android.imagic.files.filelist.FileListActivity
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.storage.CloudLogoutDialogActivity
import com.android.imagic.files.storage.CloudLogoutFragment
import com.android.imagic.files.storage.EditStarBoardDriveServerFragment
import com.android.imagic.files.storage.StarBoardDriveServer
import com.android.imagic.files.util.Constants
import com.android.imagic.files.util.createIntent
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.valueCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LoginWidgetUpdater {

    const val EXTRA_CLOUD_AUTHORITY = "extra_cloud_authority"

    fun updateWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, LauncherLoginWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val storages = Settings.STORAGES.valueCompat.toList()
        val starBoardDriveServers = storages.filter { it is StarBoardDriveServer }
        val isLogin = starBoardDriveServers.isNotEmpty()
        Log.d("chengluo", "onUpdate isLogin: $isLogin")

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_launcher_login).apply {
            setOnClickPendingIntent(R.id.widgetLoginButton, getActivityPendingIntent(context, true))
            setOnClickPendingIntent(R.id.loginActonButton, getActivityPendingIntent(context, false))
        }

        remoteViews.setViewVisibility(
            R.id.needLoginLayout,
            if (isLogin) View.GONE else View.VISIBLE
        )
        remoteViews.setViewVisibility(
            R.id.loginSuccessLayout,
            if (!isLogin) View.GONE else View.VISIBLE
        )

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)

        starBoardDriveServers.firstOrNull()?.let { server ->
            val starBoardDriveServer = server as StarBoardDriveServer
            val authority = starBoardDriveServer.authority
            remoteViews.setTextViewText(R.id.usernameText, authority.username)

            val goToCloudListIntent = Intent(context, FileListActivity::class.java).apply {
                putExtra(EXTRA_CLOUD_AUTHORITY, authority)
            }
            val goToCloudListPendingIntent = PendingIntent.getActivity(
                context, 0, goToCloudListIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val logoutIntent = CloudLogoutDialogActivity::class.createIntent()
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putArgs(CloudLogoutFragment.Args(starBoardDriveServer))
            val logoutPendingIntent = PendingIntent.getActivity(
                context, 0, logoutIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.gotToDriveButton, goToCloudListPendingIntent)
            remoteViews.setOnClickPendingIntent(R.id.logoutButton, logoutPendingIntent)

            val max = authority.totalSpace.toInt()
            val used = authority.usedSpace.toInt()
            Log.d("chengluo", "updateWidgets setProgressbar max: $max used: $used")
            remoteViews.setProgressBar(R.id.progressbar, max, used, false)

            val usedSpace = authority.usedSpace * 1024
            val totalSpace = authority.totalSpace * 1024
            val usedSpaceString = usedSpace.asFileSize().formatHumanReadable(context)
            val totalSpaceString = totalSpace.asFileSize().formatHumanReadable(context)
            val spaceText = "$usedSpaceString / $totalSpaceString"
            remoteViews.setTextViewText(R.id.storageSpaceText, spaceText)

            appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)

            if (authority.headerImageUrl != null && authority.headerImageUrl != Constants.MS_GRAPH_PHOTO) {
                loadUserHeaderImage(
                    remoteViews,
                    authority.headerImageUrl,
                    context,
                    appWidgetManager,
                    appWidgetIds
                )
            }
        }
    }

    private fun getActivityPendingIntent(context: Context, qrcodeLogin: Boolean): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            WidgetLoginLaunchActivity::class.createIntent().putArgs(
                EditStarBoardDriveServerFragment.Args(
                    fromWidget = true,
                    qrcodeLogin = qrcodeLogin
                )
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun loadUserHeaderImage(
        remoteViews: RemoteViews,
        headerImageUrl: String,
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) = CoroutineScope(Dispatchers.IO).launch {
        Log.d("chengluo", "loadUserHeaderImage headerImageUrl: $headerImageUrl")
        val bitmap = Coil
            .imageLoader(context)
            .execute(
                ImageRequest.Builder(context)
                    .data(headerImageUrl)
                    .allowHardware(false)
                    .build()
            ).drawable?.toBitmap()
        bitmap?.let {
            remoteViews.setImageViewBitmap(R.id.userHeaderImage, bitmap)
        }
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
    }
}