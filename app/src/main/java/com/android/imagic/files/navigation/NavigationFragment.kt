package com.android.imagic.files.navigation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.android.imagic.files.R
import com.android.imagic.files.appwidget.LoginWidgetUpdater
import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.databinding.NewNavigationFragmentBinding
import com.android.imagic.files.file.QuickAccessWrap
import com.android.imagic.files.filelist.CloudPathData
import com.android.imagic.files.filelist.FileListActivity
import com.android.imagic.files.filelist.FileModeState
import com.android.imagic.files.storage.CloudLogoutFragment
import com.android.imagic.files.storage.EditStarBoardDriveServerActivity
import com.android.imagic.files.storage.EditStarBoardDriveServerFragment
import com.android.imagic.files.storage.GoogleDriveServer
import com.android.imagic.files.storage.OneDriveServer
import com.android.imagic.files.storage.StarBoardDriveServer
import com.android.imagic.files.storage.Storages
import com.android.imagic.files.util.Constants
import com.android.imagic.files.util.createIntent
import com.android.imagic.files.util.launchSafe
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.startActivitySafe
import com.android.imagic.files.util.valueCompat
import com.android.imagic.files.util.viewModels
import java8.nio.file.Path
import java8.nio.file.Paths

class NavigationFragment : Fragment(), NavigationItem.Listener, StorageListAdapter.Listener {

    private val viewModel by viewModels { { NavigationViewModel() } }

    private val openPathLauncher = registerForActivityResult(
        FileListActivity.OpenDirectoryContract(), ::onOpenPathResult
    )

    private lateinit var binding: NewNavigationFragmentBinding

    private lateinit var storageListAdapter: StorageListAdapter
    private lateinit var quickAccessListAdapter: QuickAccessListAdapter
    private lateinit var settingsListAdapter: SettingsListAdapter

    lateinit var listener: Listener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = NewNavigationFragmentBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // TODO: Needed?
        //binding.recyclerView.setItemAnimator(new NoChangeAnimationItemAnimator())
        val context = requireContext()
        binding.quickAccessRecyclerView.layoutManager =
            GridLayoutManager(context, 4, GridLayoutManager.VERTICAL, false)
        quickAccessListAdapter = QuickAccessListAdapter(this, context)
        binding.quickAccessRecyclerView.adapter = quickAccessListAdapter
        binding.quickAccessRecyclerView.setMaxHeight(500)

        binding.storageRecyclerView.layoutManager = LinearLayoutManager(context)
        storageListAdapter = StorageListAdapter(this, this, context)
        binding.storageRecyclerView.adapter = storageListAdapter
        binding.storageRecyclerView.setMaxHeight(600)

        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(context)
        settingsListAdapter = SettingsListAdapter(this, context)
        binding.settingsRecyclerView.adapter = settingsListAdapter
        binding.settingsRecyclerView.setMaxHeight(500)

        binding.loginLayout.setOnClickListener {
            val sDriveServer = viewModel.starBoardDriveServer.valueCompat
            if (sDriveServer == null) {
                launchIntent(
                    EditStarBoardDriveServerActivity::class.createIntent().putArgs(
                        EditStarBoardDriveServerFragment.Args(
                            fromWidget = false,
                            qrcodeLogin = true
                        )
                    )
                )
            } else {
                logout(sDriveServer)
            }
        }

        val viewLifecycleOwner = viewLifecycleOwner
        StorageItemListLiveData.observe(viewLifecycleOwner) { onStorageItemsChanged(it) }
        QuickAccessItemListLiveData.observe(viewLifecycleOwner) { onQuickAccessItemsChanged(it) }
        SettingsItemListLiveData.observe(viewLifecycleOwner) { onSettingsItemsChanged(it) }
        viewModel.starBoardDriveServer.observe(viewLifecycleOwner) {
            onStarBoardDriveServerChanged(it)
        }
        viewModel.serverAuthority.observe(viewLifecycleOwner) {
            onServerAuthorityChanged(it)
        }
        listener.observeCurrentPath(viewLifecycleOwner) { onCurrentPathChanged(it) }
        listener.observeCurrentQuickAccessCategory(viewLifecycleOwner) {
            onQuickAccessCategoryChanged(it)
        }
    }

    private fun onOpenPathResult(result: Path?) {
        result ?: return
        val bookmarkDirectory = BookmarkDirectory(null, result)
        launchIntent(
            AddBookmarkDirectoryDialogActivity::class.createIntent()
                .putArgs(AddBookmarkDirectoryDialogFragment.Args(bookmarkDirectory))
        )
    }

    private fun onStorageItemsChanged(navigationItems: List<NavigationItem?>) {
        storageListAdapter.replace(navigationItems)
        storageListAdapter.notifyCheckedChanged()
        quickAccessListAdapter.notifyCheckedChanged()
        viewModel.onStorageItemsChanged(navigationItems, requireContext())
    }

    private fun onQuickAccessItemsChanged(navigationItems: List<NavigationItem?>) {
        quickAccessListAdapter.replace(navigationItems)
        binding.quickAccessRecyclerView.isVisible = navigationItems.isNotEmpty()
    }

    private fun onSettingsItemsChanged(navigationItems: List<NavigationItem?>) {
        settingsListAdapter.replace(navigationItems)
    }

    private fun onCurrentPathChanged(path: Path) {
        storageListAdapter.notifyCheckedChanged()
        quickAccessListAdapter.notifyCheckedChanged()
    }

    private fun onQuickAccessCategoryChanged(quickAccessCategory: QuickAccessCategory?) {
        storageListAdapter.notifyCheckedChanged()
        quickAccessListAdapter.notifyCheckedChanged()
    }

    private fun onStarBoardDriveServerChanged(server: StarBoardDriveServer?) {
        if (server == null) {
            binding.userHeaderImage.setImageResource(R.drawable.nav_user_header_img)
            binding.username.text = getString(R.string.navigation_login_account_tip)
        } else {
            val authority = server.authority
            authority.headerImageUrl?.let { url ->
                if (url != Constants.MS_GRAPH_PHOTO) {
                    binding.userHeaderImage.load(url) {
                        error(R.drawable.nav_user_header_img)
                        placeholder(R.drawable.nav_user_header_img)
                        crossfade(true)
                    }
                } else {
                    binding.userHeaderImage.setImageResource(R.drawable.nav_user_header_img)
                }
                LoginWidgetUpdater.updateWidgets(requireContext())
            }
            binding.username.text = authority.username
        }
    }

    private fun onServerAuthorityChanged(serverAuthority: ServerAuthority?) {
        Log.d("chengluo", "onServerAuthorityChanged: $serverAuthority")
        if (serverAuthority == null) return
        if (serverAuthority.isGoogle) {
            Storages.addOrReplace(GoogleDriveServer(null, null, serverAuthority))
        }
        if (serverAuthority.isMicrosoft) {
            Storages.addOrReplace(OneDriveServer(null, null, serverAuthority))
        }
    }

    override val currentPath: Path
        get() = listener.currentPath

    override val currentFileMode: FileModeState
        get() = listener.currentFileMode

    override fun navigateToCloud(authority: Authority) {
        listener.setFileModeState(FileModeState.StarBoardDrive(authority))
    }

    override fun navigateToGoogleDrive(authority: ServerAuthority) {
        listener.setFileModeState(FileModeState.GoogleDrive(authority))
    }

    override fun navigateToOneDrive(authority: ServerAuthority) {
        listener.setFileModeState(FileModeState.OneDrive(authority))
    }

    override fun navigateTo(path: Path) {
        listener.navigateTo(path)
    }

    override fun navigateToRoot(path: Path) {
        listener.navigateToRoot(path)
    }

    override fun quickAccessTo(quickAccess: QuickAccess) {
        if (quickAccess.category == QuickAccessCategory.DOWNLOAD) {
            val path = Paths.get(getExternalStorageDirectory(quickAccess.relativePath))
            navigateToRoot(path)
        }
        val quickAccessWrap = QuickAccessWrap(quickAccess, requireContext())
        listener.setFileModeState(FileModeState.QuickAccess(quickAccessWrap))
    }

    override fun launchIntent(intent: Intent) {
        startActivitySafe(intent)
    }

    override fun closeNavigationDrawer() {
        listener.closeNavigationDrawer()
    }

    override fun addBookmark() {
        openPathLauncher.launchSafe(currentPath, this)
    }

    override fun cancelDragDrop() {
        listener.cancelDragDrop()
    }

    override fun dropToLocal(directoryPath: Path) {
        listener.dropToLocal(directoryPath)
    }

    override fun dropToStarBoardDrive(
        cloudDirectoryPath: CloudPathData,
        authority: Authority
    ) {
        listener.dropToStarBoardDrive(cloudDirectoryPath, authority)
    }

    override fun dropToOneDrive(
        cloudDirectoryPath: CloudPathData,
        authority: ServerAuthority
    ) {
        listener.dropToOneDrive(cloudDirectoryPath, authority)
    }

    override fun dropToGoogleDrive(
        cloudDirectoryPath: CloudPathData,
        authority: ServerAuthority
    ) {
        listener.dropToGoogleDrive(cloudDirectoryPath, authority)
    }

    override fun logout(server: StarBoardDriveServer) {
        CloudLogoutFragment.show(server, this)
    }

    fun logout() {
        listener.logout()
    }

    interface Listener {
        val currentPath: Path
        val currentFileMode: FileModeState
        fun setFileModeState(mode: FileModeState)
        fun navigateTo(path: Path)
        fun navigateToRoot(path: Path)
        fun navigateToDefaultRoot()
        fun observeCurrentPath(owner: LifecycleOwner, observer: (Path) -> Unit)
        fun observeCurrentQuickAccessCategory(
            owner: LifecycleOwner,
            observer: (QuickAccessCategory?) -> Unit
        )

        fun closeNavigationDrawer()
        fun cancelDragDrop()
        fun dropToLocal(directoryPath: Path)
        fun dropToStarBoardDrive(cloudDirectoryPath: CloudPathData, authority: Authority)
        fun dropToOneDrive(cloudDirectoryPath: CloudPathData, authority: ServerAuthority)
        fun dropToGoogleDrive(cloudDirectoryPath: CloudPathData, authority: ServerAuthority)
        fun logout()
    }
}
