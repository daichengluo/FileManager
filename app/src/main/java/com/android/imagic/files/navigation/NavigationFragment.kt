package com.android.imagic.files.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.imagic.files.databinding.NewNavigationFragmentBinding
import com.android.imagic.files.file.QuickAccessWrap
import com.android.imagic.files.filelist.FileListActivity
import com.android.imagic.files.filelist.FileModeState
import com.android.imagic.files.provider.cloud.sdrive.client.Authority
import com.android.imagic.files.util.createIntent
import com.android.imagic.files.util.launchSafe
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.startActivitySafe
import java8.nio.file.Path
import java8.nio.file.Paths

class NavigationFragment : Fragment(), NavigationItem.Listener {
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
        storageListAdapter = StorageListAdapter(this, context)
        binding.storageRecyclerView.adapter = storageListAdapter
        binding.storageRecyclerView.setMaxHeight(600)

        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(context)
        settingsListAdapter = SettingsListAdapter(this, context)
        binding.settingsRecyclerView.adapter = settingsListAdapter
        binding.settingsRecyclerView.setMaxHeight(500)

        binding.loginLayout.isVisible = false
        binding.loginLayout.setOnClickListener {
            // todo login action
        }

        val viewLifecycleOwner = viewLifecycleOwner
        StorageItemListLiveData.observe(viewLifecycleOwner) { onStorageItemsChanged(it) }
        QuickAccessItemListLiveData.observe(viewLifecycleOwner) { onQuickAccessItemsChanged(it) }
        SettingsItemListLiveData.observe(viewLifecycleOwner) { onSettingsItemsChanged(it) }
        listener.observeCurrentPath(viewLifecycleOwner) { onCurrentPathChanged(it) }
        listener.observeCurrentQuickAccessCategory(viewLifecycleOwner) {
            onQuickAccessCategoryChanged(
                it
            )
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

    override val currentPath: Path
        get() = listener.currentPath

    override val currentFileMode: FileModeState
        get() = listener.currentFileMode

    override fun navigateToCloud(authority: Authority) {
        listener.setFileModeState(FileModeState.Cloud(authority))
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
    }
}
