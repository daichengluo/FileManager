package com.android.imagic.files.navigation

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.storage.StorageVolume
import androidx.annotation.DrawableRes
import androidx.annotation.Size
import androidx.annotation.StringRes
import com.android.imagic.files.R
import com.android.imagic.files.about.AboutActivity
import com.android.imagic.files.compat.getDescriptionCompat
import com.android.imagic.files.compat.isPrimaryCompat
import com.android.imagic.files.compat.pathCompat
import com.android.imagic.files.file.JavaFile
import com.android.imagic.files.file.asFileSize
import com.android.imagic.files.filelist.FileModeState
import com.android.imagic.files.ftpserver.FtpServerActivity
import com.android.imagic.files.settings.QuickAccessListActivity
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.settings.SettingsActivity
import com.android.imagic.files.settings.StandardDirectoryListActivity
import com.android.imagic.files.storage.AddStorageDialogActivity
import com.android.imagic.files.storage.FileSystemRoot
import com.android.imagic.files.storage.GDriveServer
import com.android.imagic.files.storage.OneDriveServer
import com.android.imagic.files.storage.SDriveServer
import com.android.imagic.files.storage.Storage
import com.android.imagic.files.storage.StorageVolumeListLiveData
import com.android.imagic.files.util.createIntent
import com.android.imagic.files.util.isMounted
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.supportsExternalStorageManager
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path
import java8.nio.file.Paths

val navigationStorageItems: List<NavigationItem>
    get() = mutableListOf<NavigationItem>().apply {
        addAll(storageItems)
        if (Environment::class.supportsExternalStorageManager()) {
            // Starting with R, we can get read/write access to non-primary storage volumes with
            // MANAGE_EXTERNAL_STORAGE. However before R, we only have read-only access to them
            // and need to use the Storage Access Framework instead, so hide them in this case
            // to avoid confusion.
            addAll(storageVolumeItems)
        }
        add(AddStorageItem())
    }

val navigationBookmarkItems: List<NavigationItem>
    get() = mutableListOf<NavigationItem>().apply {
        val bookmarkDirectoryItems = bookmarkDirectoryItems
        if (bookmarkDirectoryItems.isNotEmpty()) {
            addAll(bookmarkDirectoryItems)
        }
    }

val navigationSettingsItems: List<NavigationItem>
    get() = mutableListOf<NavigationItem>().apply {
        addAll(menuItems)
    }

val navigationItems: List<NavigationItem?>
    get() = mutableListOf<NavigationItem?>().apply {
        addAll(storageItems)
        if (Environment::class.supportsExternalStorageManager()) {
            // Starting with R, we can get read/write access to non-primary storage volumes with
            // MANAGE_EXTERNAL_STORAGE. However before R, we only have read-only access to them
            // and need to use the Storage Access Framework instead, so hide them in this case
            // to avoid confusion.
            addAll(storageVolumeItems)
        }
        add(AddStorageItem())
        val standardDirectoryItems = standardDirectoryItems
        if (standardDirectoryItems.isNotEmpty()) {
            add(null)
            addAll(standardDirectoryItems)
        }
        val bookmarkDirectoryItems = bookmarkDirectoryItems
        if (bookmarkDirectoryItems.isNotEmpty()) {
            add(null)
            addAll(bookmarkDirectoryItems)
        }
        add(null)
        addAll(menuItems)
    }

private val storageItems: List<NavigationItem>
    @Size(min = 0)
    get() = Settings.STORAGES.valueCompat.filter { it.isVisible }.map {
        if (it.path != null) PathStorageItem(it) else {
            when (it) {
                is SDriveServer -> CloudStorageItem(it)
                is GDriveServer -> GoogleDriveItem(it)
                is OneDriveServer -> OneDriveItem(it)
                else -> IntentStorageItem(it)
            }
        }
    }

internal abstract class PathItem(val path: Path) : NavigationItem() {

    override fun isChecked(listener: Listener): Boolean {
        return listener.currentFileMode is FileModeState.Local && listener.currentPath == path
    }

    override fun onClick(listener: Listener) {
        if (this is NavigationRoot) {
            listener.navigateToRoot(path)
        } else {
            listener.navigateTo(path)
        }
        listener.closeNavigationDrawer()
    }
}

private class PathStorageItem(
    private val storage: Storage
) : PathItem(storage.path!!), NavigationRoot {
    init {
        require(storage.isVisible)
    }

    override val id: Long
        get() = storage.id

    override val iconRes: Int
        @DrawableRes
        get() = storage.iconRes

    override fun getTitle(context: Context): String = storage.getName(context)

    override fun getSubtitle(context: Context): String? =
        storage.linuxPath?.let { getStorageSubtitle(it, context) }

    override fun onLongClick(listener: Listener): Boolean {
        listener.launchIntent(storage.createEditIntent())
        return true
    }

    override fun getName(context: Context): String = getTitle(context)
}

private class IntentStorageItem(
    private val storage: Storage
) : NavigationItem() {
    init {
        require(storage.isVisible)
    }

    override val id: Long
        get() = storage.id

    override val iconRes: Int
        @DrawableRes
        get() = storage.iconRes

    override fun getTitle(context: Context): String = storage.getName(context)

    override fun onClick(listener: Listener) {
        listener.launchIntent(storage.createIntent()!!)
        listener.closeNavigationDrawer()
    }

    override fun onLongClick(listener: Listener): Boolean {
        listener.launchIntent(storage.createEditIntent())
        return true
    }
}

internal class CloudStorageItem(
    val storage: SDriveServer
) : NavigationItem() {
    init {
        require(storage.isVisible)
    }

    override val id: Long
        get() = storage.id

    override val iconRes: Int
        @DrawableRes
        get() = storage.iconRes

    override fun getTitle(context: Context): String = storage.getName(context)

    override fun getSubtitle(context: Context): String? {
        val freeSpace = storage.authority.freeSpace * 1024
        val totalSpace = storage.authority.totalSpace * 1024
        val freeSpaceString = freeSpace.asFileSize().formatHumanReadable(context)
        val totalSpaceString = totalSpace.asFileSize().formatHumanReadable(context)
        return context.getString(
            R.string.navigation_storage_subtitle_format, freeSpaceString, totalSpaceString
        )
    }

    override fun isChecked(listener: Listener): Boolean {
        val mode = listener.currentFileMode
        return if (mode is FileModeState.Cloud) {
            mode.authority == storage.authority
        } else false
    }

    override fun onClick(listener: Listener) {
        listener.navigateToCloud(storage.authority)
    }

    override fun onLongClick(listener: Listener): Boolean {
        listener.logout(storage)
        return true
    }
}

internal class GoogleDriveItem(
    val storage: GDriveServer
) : NavigationItem() {
    init {
        require(storage.isVisible)
    }

    override val id: Long
        get() = storage.id

    override val iconRes: Int
        @DrawableRes
        get() = storage.iconRes

    override fun getTitle(context: Context): String = storage.getName(context)

    override fun getSubtitle(context: Context): String? = null


    override fun onClick(listener: Listener) {
        listener.navigateToGoogleDrive(storage.accessToken)
    }

    override fun onLongClick(listener: Listener): Boolean {
        return true
    }
}

internal class OneDriveItem(
    val storage: OneDriveServer
) : NavigationItem() {
    init {
        require(storage.isVisible)
    }

    override val id: Long
        get() = storage.id

    override val iconRes: Int
        @DrawableRes
        get() = storage.iconRes

    override fun getTitle(context: Context): String = storage.getName(context)

    override fun getSubtitle(context: Context): String? = null

    override fun onClick(listener: Listener) {
        listener.navigateToOneDrive(storage.accessToken)
    }

    override fun onLongClick(listener: Listener): Boolean {
        return true
    }
}

private val storageVolumeItems: List<NavigationItem>
    @Size(min = 0)
    get() = StorageVolumeListLiveData.valueCompat.filter { !it.isPrimaryCompat && it.isMounted }
        .map { StorageVolumeItem(it) }

internal class StorageVolumeItem(
    private val storageVolume: StorageVolume
) : PathItem(Paths.get(storageVolume.pathCompat)), NavigationRoot {

    override val id: Long
        get() = storageVolume.hashCode().toLong()

    override val iconRes: Int
        @DrawableRes
        get() = if (storageVolume.isRemovable)
            R.drawable.nav_storage_usb_device
        else
            R.drawable.nav_storage_internal

    override fun getTitle(context: Context): String = storageVolume.getDescriptionCompat(context)

    override fun getSubtitle(context: Context): String? =
        getStorageSubtitle(storageVolume.pathCompat, context)

    override fun getName(context: Context): String = getTitle(context)
}

private fun getStorageSubtitle(linuxPath: String, context: Context): String? {
    var totalSpace = JavaFile.getTotalSpace(linuxPath)
    val freeSpace: Long
    when {
        totalSpace != 0L -> freeSpace = JavaFile.getFreeSpace(linuxPath)
        linuxPath == FileSystemRoot.LINUX_PATH -> {
            // Root directory may not be an actual partition on legacy Android versions (can be
            // a ramdisk instead). On modern Android the system partition will be mounted as
            // root instead so let's try with the system partition again.
            // @see https://source.android.com/devices/bootloader/system-as-root
            val systemPath = Environment.getRootDirectory().path
            totalSpace = JavaFile.getTotalSpace(systemPath)
            freeSpace = JavaFile.getFreeSpace(systemPath)
        }

        else -> freeSpace = 0
    }
    if (totalSpace == 0L) {
        return null
    }
    val freeSpaceString = freeSpace.asFileSize().formatHumanReadable(context)
    val totalSpaceString = totalSpace.asFileSize().formatHumanReadable(context)
    return context.getString(
        R.string.navigation_storage_subtitle_format, freeSpaceString, totalSpaceString
    )
}

internal class AddStorageItem : NavigationItem() {
    override val id: Long = R.string.navigation_add_storage.toLong()

    @DrawableRes
    override val iconRes: Int = R.drawable.nav_storage_add

    override fun getTitle(context: Context): String =
        context.getString(R.string.navigation_add_storage)

    override fun onClick(listener: Listener) {
        listener.launchIntent(AddStorageDialogActivity::class.createIntent())
    }
}

val quickAccessItems: List<NavigationItem>
    @Size(min = 0)
    get() = QuickAccessesLiveData.valueCompat
        .filter { it.isEnabled }
        .map { QuickAccessItem(it) }

private class QuickAccessItem(
    private val quickAccess: QuickAccess
) : NavigationItem() {
    init {
        require(quickAccess.isEnabled)
    }

    override fun isChecked(listener: Listener): Boolean {
        val mode = listener.currentFileMode
        return if (mode is FileModeState.QuickAccess) {
            val currentQuickAccess = mode.quickAccessWrap.quickAccess
            currentQuickAccess == quickAccess
        } else false
    }

    override val id: Long
        get() = quickAccess.id

    override val iconRes: Int
        @DrawableRes
        get() = quickAccess.iconRes

    override fun getTitle(context: Context): String = quickAccess.getTitle(context)

    override fun onClick(listener: Listener) = listener.quickAccessTo(quickAccess)

    override fun onLongClick(listener: Listener): Boolean {
        listener.launchIntent(QuickAccessListActivity::class.createIntent())
        return true
    }
}

val quickAccesses: List<QuickAccess>
    get() {
        val settingsMap = Settings.QUICK_ACCESS_SETTINGS.valueCompat.associateBy { it.id }
        return defaultQuickAccesses.map {
            val settings = settingsMap[it.key]
            if (settings != null) it.withSettings(settings) else it
        }
    }

private val defaultQuickAccesses: List<QuickAccess>
    get() = DEFAULT_QUICK_ACCESSES

private val DEFAULT_QUICK_ACCESSES = listOf(
    QuickAccess(
        iconRes = R.drawable.quick_access_download_icon,
        titleRes = R.string.navigation_standard_directory_downloads,
        category = QuickAccessCategory.DOWNLOAD,
        relativePath = Environment.DIRECTORY_DOWNLOADS
    ),
    QuickAccess(
        iconRes = R.drawable.file_image_icon,
        titleRes = R.string.navigation_quick_access_images,
        category = QuickAccessCategory.IMAGE,
        relativePath = Environment.DIRECTORY_PICTURES
    ),
    QuickAccess(
        iconRes = R.drawable.file_video_icon,
        titleRes = R.string.navigation_quick_access_videos,
        category = QuickAccessCategory.VIDEO,
        relativePath = Environment.DIRECTORY_MOVIES
    ),
    QuickAccess(
        iconRes = R.drawable.file_audio_icon,
        titleRes = R.string.navigation_quick_access_audio,
        category = QuickAccessCategory.AUDIO,
        relativePath = Environment.DIRECTORY_MUSIC
    ),
    QuickAccess(
        iconRes = R.drawable.file_document_icon,
        titleRes = R.string.navigation_standard_directory_documents,
        category = QuickAccessCategory.DOCUMENT,
        relativePath = Environment.DIRECTORY_DOCUMENTS
    ),
    QuickAccess(
        iconRes = R.drawable.file_apk_icon,
        titleRes = R.string.navigation_quick_access_apk,
        category = QuickAccessCategory.APK,
        relativePath = QuickAccess.QUICK_ACCESS_APK
    )
)

private val standardDirectoryItems: List<NavigationItem>
    @Size(min = 0)
    get() =
        StandardDirectoriesLiveData.valueCompat
            .filter { it.isEnabled }
            .map { StandardDirectoryItem(it) }

private class StandardDirectoryItem(
    private val standardDirectory: StandardDirectory
) : PathItem(Paths.get(getExternalStorageDirectory(standardDirectory.relativePath))) {
    init {
        require(standardDirectory.isEnabled)
    }

    override val id: Long
        get() = standardDirectory.id

    override val iconRes: Int
        @DrawableRes
        get() = standardDirectory.iconRes

    override fun getTitle(context: Context): String = standardDirectory.getTitle(context)

    override fun onLongClick(listener: Listener): Boolean {
        listener.launchIntent(StandardDirectoryListActivity::class.createIntent())
        return true
    }
}

val standardDirectories: List<StandardDirectory>
    get() {
        val settingsMap = Settings.STANDARD_DIRECTORY_SETTINGS.valueCompat.associateBy { it.id }
        return defaultStandardDirectories.map {
            val settings = settingsMap[it.key]
            if (settings != null) it.withSettings(settings) else it
        }
    }

private val defaultStandardDirectories: List<StandardDirectory>
    get() = DEFAULT_STANDARD_DIRECTORIES

// @see android.os.Environment#STANDARD_DIRECTORIES
private val DEFAULT_STANDARD_DIRECTORIES = listOf(
    StandardDirectory(
        R.drawable.quick_access_download_icon,
        R.string.navigation_standard_directory_downloads,
        Environment.DIRECTORY_DOWNLOADS,
        true
    ),
    StandardDirectory(
        R.drawable.file_image_icon,
        R.string.navigation_quick_access_images,
        Environment.DIRECTORY_PICTURES,
        true
    ),
    StandardDirectory(
        R.drawable.file_video_icon,
        R.string.navigation_quick_access_videos,
        Environment.DIRECTORY_MOVIES,
        true
    ),
    StandardDirectory(
        R.drawable.file_audio_icon,
        R.string.navigation_quick_access_audio,
        Environment.DIRECTORY_MUSIC,
        true
    ),
    StandardDirectory(
        R.drawable.file_document_icon,
        R.string.navigation_standard_directory_documents,
        Environment.DIRECTORY_DOCUMENTS,
        true
    )
)

internal fun getExternalStorageDirectory(relativePath: String): String =
    @Suppress("DEPRECATION")
    Environment.getExternalStoragePublicDirectory(relativePath).path

private val bookmarkDirectoryItems: List<NavigationItem>
    @Size(min = 0)
    get() = Settings.BOOKMARK_DIRECTORIES.valueCompat.map { BookmarkDirectoryItem(it) }

private class BookmarkDirectoryItem(
    private val bookmarkDirectory: BookmarkDirectory
) : PathItem(bookmarkDirectory.path) {
    // We cannot simply use super.getId() because different bookmark directories may have
    // the same path.
    override val id: Long
        get() = bookmarkDirectory.id

    @DrawableRes
    override val iconRes: Int = R.drawable.file_directory_icon

    override fun getTitle(context: Context): String = bookmarkDirectory.name

    override fun onLongClick(listener: Listener): Boolean {
        listener.launchIntent(
            EditBookmarkDirectoryDialogActivity::class.createIntent()
                .putArgs(EditBookmarkDirectoryDialogFragment.Args(bookmarkDirectory))
        )
        return true
    }
}

private class AddBookmarkItem : NavigationItem() {
    override val id: Long = R.string.file_list_action_add_bookmark.toLong()

    @DrawableRes
    override val iconRes: Int = R.drawable.nav_add_bookmark

    override fun getTitle(context: Context): String =
        context.getString(R.string.file_list_action_add_bookmark)

    override fun onClick(listener: Listener) {
        listener.addBookmark()
    }
}

private val menuItems: List<NavigationItem>
    @Size(3)
    get() = listOf(
        IntentMenuItem(
            R.drawable.shared_directory_icon_white_24dp, R.string.navigation_ftp_server,
            FtpServerActivity::class.createIntent()
        ),
        IntentMenuItem(
            R.drawable.settings_icon_white_24dp, R.string.navigation_settings,
            SettingsActivity::class.createIntent()
        ),
        IntentMenuItem(
            R.drawable.about_icon_white_24dp, R.string.navigation_about,
            AboutActivity::class.createIntent()
        )
    )

private abstract class MenuItem(
    @DrawableRes override val iconRes: Int,
    @StringRes val titleRes: Int
) : NavigationItem() {
    override fun getTitle(context: Context): String = context.getString(titleRes)
}

private class IntentMenuItem(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    private val intent: Intent
) : MenuItem(iconRes, titleRes) {
    override val id: Long
        get() = intent.component.hashCode().toLong()

    override fun onClick(listener: Listener) {
        listener.launchIntent(intent)
        listener.closeNavigationDrawer()
    }
}
