package com.android.imagic.files.appwidget

import android.annotation.SuppressLint
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
import com.android.imagic.files.appwidget.receiver.RefreshQRCodeReceiver
import com.android.imagic.files.cloud.CloudLoginFragment
import com.android.imagic.files.cloud.CloudLogoutDialogActivity
import com.android.imagic.files.cloud.CloudLogoutFragment
import com.android.imagic.files.file.asFileSize
import com.android.imagic.files.filelist.FileListActivity
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.storage.GoogleDriveServer
import com.android.imagic.files.storage.OneDriveServer
import com.android.imagic.files.storage.StarBoardDriveServer
import com.android.imagic.files.util.Constants
import com.android.imagic.files.util.NetworkUtil
import com.android.imagic.files.util.QrcodeUtil
import com.android.imagic.files.util.createIntent
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.valueCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Clock

object LoginWidgetUpdater {

    const val EXTRA_STARBOARD_AUTHORITY = "extra_starboard_authority"
    const val EXTRA_ONEDRIVE_AUTHORITY = "extra_onedrive_authority"
    const val EXTRA_GOOGLE_DRIVE_AUTHORITY = "extra_google_drive_authority"

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
        val starBoardDriveServers = storages.filterIsInstance<StarBoardDriveServer>()
        val googleDriveServers = storages.filterIsInstance<GoogleDriveServer>()
        val oneDriveServers = storages.filterIsInstance<OneDriveServer>()
        val isLogin = starBoardDriveServers.isNotEmpty()

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_launcher_login)
        remoteViews.setOnClickPendingIntent(
            R.id.widgetLoginButton,
            getActivityPendingIntent(context)
        )

        remoteViews.setViewVisibility(
            R.id.needLoginLayout,
            if (isLogin) View.GONE else View.VISIBLE
        )
        remoteViews.setViewVisibility(
            R.id.loginSuccessLayout,
            if (isLogin) View.VISIBLE else View.GONE
        )

        val refreshQRCodeIntent = Intent(
            context,
            RefreshQRCodeReceiver::class.java
        ).apply {
            action = RefreshQRCodeReceiver.ACTION_REFRESH_QRCODE
        }
        val refreshQRCodePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            refreshQRCodeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.refreshQRCode, refreshQRCodePendingIntent)

        if (!isLogin) {
            loadQRCodeImage(remoteViews, context, appWidgetManager, appWidgetIds)
        }
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)

        oneDriveServers.firstOrNull()?.let { server ->
            val authority = server.authority

            remoteViews.setImageViewResource(R.id.thirdDriveIcon, R.drawable.widget_microsoft_icon)
            remoteViews.setTextViewText(
                R.id.thirdDriveName,
                context.getString(R.string.one_drive)
            )
            val intent = Intent(context, FileListActivity::class.java).apply {
                putExtra(EXTRA_ONEDRIVE_AUTHORITY, authority)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, authority.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val progress = (authority.usedSpace / authority.totalSpace).toInt()
            Log.d("chengluo", "updateWidgets oneDrive setProgressbar progress: $progress")
            remoteViews.setProgressBar(R.id.thirdDriveProgressbar, 100, progress, false)

            val usedSpace = authority.usedSpace
            val totalSpace = authority.totalSpace
            val usedSpaceString = usedSpace.asFileSize().formatHumanReadable(context)
            val totalSpaceString = totalSpace.asFileSize().formatHumanReadable(context)
            val spaceText = "$usedSpaceString / $totalSpaceString"
            remoteViews.setTextViewText(R.id.thirdDriveSpaceText, spaceText)
            remoteViews.setOnClickPendingIntent(R.id.thirdDriveLayout, pendingIntent)
            remoteViews.setViewVisibility(R.id.thirdDriveLayout, View.VISIBLE)
        }

        googleDriveServers.firstOrNull()?.let { server ->
            val authority = server.authority

            remoteViews.setImageViewResource(R.id.thirdDriveIcon, R.drawable.widget_google_icon)
            remoteViews.setTextViewText(
                R.id.thirdDriveName,
                context.getString(R.string.google_drive)
            )
            val intent = Intent(context, FileListActivity::class.java).apply {
                putExtra(EXTRA_GOOGLE_DRIVE_AUTHORITY, authority)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, authority.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val progress = (authority.usedSpace / authority.totalSpace).toInt()
            Log.d("chengluo", "updateWidgets googleDrive setProgressbar progress: $progress")
            remoteViews.setProgressBar(R.id.thirdDriveProgressbar, 100, progress, false)

            val usedSpace = authority.usedSpace
            val totalSpace = authority.totalSpace
            val usedSpaceString = usedSpace.asFileSize().formatHumanReadable(context)
            val totalSpaceString = totalSpace.asFileSize().formatHumanReadable(context)
            val spaceText = "$usedSpaceString / $totalSpaceString"
            Log.d(
                "chengluo",
                "updateWidgets googleDrive usedSpaceString: $usedSpaceString totalSpaceString: $totalSpaceString"
            )
            remoteViews.setTextViewText(R.id.thirdDriveSpaceText, spaceText)
            remoteViews.setOnClickPendingIntent(R.id.thirdDriveLayout, pendingIntent)
            remoteViews.setViewVisibility(R.id.thirdDriveLayout, View.VISIBLE)
        }

        starBoardDriveServers.firstOrNull()?.let { server ->
            val authority = server.authority
            remoteViews.setTextViewText(R.id.usernameText, authority.username)

            val intent = Intent(context, FileListActivity::class.java).apply {
                putExtra(EXTRA_STARBOARD_AUTHORITY, authority)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, authority.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val logoutIntent = CloudLogoutDialogActivity::class.createIntent()
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putArgs(CloudLogoutFragment.Args(server))
            val logoutPendingIntent = PendingIntent.getActivity(
                context, 2, logoutIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.gotToDriveButton, pendingIntent)
            remoteViews.setOnClickPendingIntent(R.id.starBoardDriveLayout, pendingIntent)
            remoteViews.setOnClickPendingIntent(R.id.logoutButton, logoutPendingIntent)

            val max = authority.totalSpace.toInt()
            val used = authority.usedSpace.toInt()
            Log.d("chengluo", "updateWidgets setProgressbar max: $max used: $used")
            remoteViews.setProgressBar(R.id.starBoardDriveProgressbar, max, used, false)

            val usedSpace = authority.usedSpace * 1024
            val totalSpace = authority.totalSpace * 1024
            val usedSpaceString = usedSpace.asFileSize().formatHumanReadable(context)
            val totalSpaceString = totalSpace.asFileSize().formatHumanReadable(context)
            val spaceText = "$usedSpaceString / $totalSpaceString"
            remoteViews.setTextViewText(R.id.starBoardDriveSpaceText, spaceText)

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

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
    }

    private fun getActivityPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            WidgetLoginLaunchActivity::class.createIntent().putArgs(
                CloudLoginFragment.Args()
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

    private fun loadQRCodeImage(
        remoteViews: RemoteViews,
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) = CoroutineScope(Dispatchers.IO).launch {
        val deviceId = NetworkUtil.getMacAddress() ?: getAndroidId(context)
        val state = generateRandomString()
        val timestamp = Clock.systemUTC().instant().epochSecond.toString()
        val content =
            "https://wboard.starboard-online.com/Long.html?Mac=$deviceId&state=$state&timeStamp=$timestamp"
        Log.d("chengluo", "loadQRCodeImage generateQrcode: $content")
        val bitmap = QrcodeUtil.generateQRCodeBitmap(content, 176)
        remoteViews.setImageViewBitmap(R.id.qrCodeImage, bitmap)
        remoteViews.setViewVisibility(R.id.qrCodeImage, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.refreshQRCode, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.qrCodeProgressBar, View.GONE)
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidId(context: Context) = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ANDROID_ID
    )

    private fun generateRandomString(length: Int = 12): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}