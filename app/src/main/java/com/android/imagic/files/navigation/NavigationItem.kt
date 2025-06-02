package com.android.imagic.files.navigation

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.compat.getDrawableCompat
import com.android.imagic.files.filelist.FileModeState
import com.android.imagic.files.storage.SDriveServer
import java8.nio.file.Path

abstract class NavigationItem {
    abstract val id: Long

    fun getIcon(context: Context): Drawable = context.getDrawableCompat(iconRes!!)

    @get:DrawableRes
    protected abstract val iconRes: Int?

    abstract fun getTitle(context: Context): String

    open fun getSubtitle(context: Context): String? = null

    open fun isChecked(listener: Listener): Boolean = false

    abstract fun onClick(listener: Listener)

    open fun onLongClick(listener: Listener): Boolean = false

    interface Listener {
        val currentPath: Path
        val currentFileMode: FileModeState
        fun navigateToCloud(authority: Authority)
        fun navigateToGoogleDrive(accessToken: String)
        fun navigateTo(path: Path)
        fun navigateToRoot(path: Path)
        fun quickAccessTo(quickAccess: QuickAccess)
        fun launchIntent(intent: Intent)
        fun closeNavigationDrawer()
        fun addBookmark()
        fun logout(server: SDriveServer)
    }
}
