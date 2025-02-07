package com.android.imagic.files.app

import android.os.AsyncTask
import android.os.Build
import android.webkit.WebView
import com.android.imagic.files.coil.initializeCoil
import com.android.imagic.files.filejob.fileJobNotificationTemplate
import com.android.imagic.files.ftpserver.ftpServerServiceNotificationTemplate
import com.android.imagic.files.hiddenapi.HiddenApi
import com.android.imagic.files.provider.FileSystemProviders
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.storage.FtpServerAuthenticator
import com.android.imagic.files.storage.SftpServerAuthenticator
import com.android.imagic.files.storage.SmbServerAuthenticator
import com.android.imagic.files.storage.StorageVolumeListLiveData
import com.android.imagic.files.storage.WebDavServerAuthenticator
import com.android.imagic.files.theme.custom.CustomThemeHelper
import com.android.imagic.files.theme.night.NightModeHelper
import jcifs.context.SingletonContext
import java.util.Properties
import com.android.imagic.files.provider.ftp.client.Client as FtpClient
import com.android.imagic.files.provider.sftp.client.Client as SftpClient
import com.android.imagic.files.provider.smb.client.Client as SmbClient
import com.android.imagic.files.provider.webdav.client.Client as WebDavClient

val appInitializers = listOf(
    ::disableHiddenApiChecks,
    ::initializeWebViewDebugging,
    ::initializeCoil,
    ::initializeFileSystemProviders,
    ::upgradeApp,
    ::initializeLiveDataObjects,
    ::initializeCustomTheme,
    ::initializeNightMode,
    ::createNotificationChannels
)

private fun initializeCrashlytics() {
    //#ifdef NONFREE
    com.android.imagic.files.nonfree.CrashlyticsInitializer.initialize()
    //#endif
}

private fun disableHiddenApiChecks() {
    HiddenApi.disableHiddenApiChecks()
}

private fun initializeWebViewDebugging() {
    if (com.android.imagic.files.BuildConfig.DEBUG) {
        WebView.setWebContentsDebuggingEnabled(true)
    }
}

private fun initializeFileSystemProviders() {
    FileSystemProviders.install()
    FileSystemProviders.overflowWatchEvents = true
    // SingletonContext.init() calls NameServiceClientImpl.initCache() which connects to network.
    AsyncTask.THREAD_POOL_EXECUTOR.execute {
        SingletonContext.init(
            Properties().apply {
                setProperty("jcifs.netbios.cachePolicy", "0")
                setProperty("jcifs.smb.client.maxVersion", "SMB1")
            }
        )
    }
    FtpClient.authenticator = FtpServerAuthenticator
    SftpClient.authenticator = SftpServerAuthenticator
    SmbClient.authenticator = SmbServerAuthenticator
    WebDavClient.authenticator = WebDavServerAuthenticator
}

private fun initializeLiveDataObjects() {
    // Force initialization of LiveData objects so that it won't happen on a background thread.
    StorageVolumeListLiveData.value
    Settings.FILE_LIST_DEFAULT_DIRECTORY.value
}

private fun initializeCustomTheme() {
    CustomThemeHelper.initialize(application)
}

private fun initializeNightMode() {
    NightModeHelper.initialize(application)
}

private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.createNotificationChannels(
            listOf(
                backgroundActivityStartNotificationTemplate.channelTemplate,
                fileJobNotificationTemplate.channelTemplate,
                ftpServerServiceNotificationTemplate.channelTemplate
            ).map { it.create(application) }
        )
    }
}
