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
import com.android.imagic.files.file.MimeTypeCategory
import com.android.imagic.files.filelist.FileListActivity
import com.android.imagic.files.util.createIntent
import com.android.imagic.files.util.launchSafe
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.startActivitySafe
import java8.nio.file.Path

class NavigationFragment : Fragment(), NavigationItem.Listener {
    private val openPathLauncher =
        registerForActivityResult(FileListActivity.OpenDirectoryContract(), ::onOpenPathResult)

    private lateinit var binding: NewNavigationFragmentBinding

    private lateinit var storageListAdapter: StorageListAdapter
    private lateinit var quickAccessListAdapter: QuickAccessListAdapter
    private lateinit var bookmarkListAdapter: BookmarkListAdapter
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

        binding.bookmarkRecyclerView.layoutManager =
            GridLayoutManager(context, 4, GridLayoutManager.VERTICAL, false)
        bookmarkListAdapter = BookmarkListAdapter(this, context)
        binding.bookmarkRecyclerView.adapter = bookmarkListAdapter
        binding.bookmarkRecyclerView.setMaxHeight(500)

        binding.storageRecyclerView.layoutManager = LinearLayoutManager(context)
        storageListAdapter = StorageListAdapter(this, context)
        binding.storageRecyclerView.adapter = storageListAdapter
        binding.storageRecyclerView.setMaxHeight(600)

        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(context)
        settingsListAdapter = SettingsListAdapter(this, context)
        binding.settingsRecyclerView.adapter = settingsListAdapter
        binding.settingsRecyclerView.setMaxHeight(500)

        binding.loginLayout.setOnClickListener {
            // todo login action
        }

        val viewLifecycleOwner = viewLifecycleOwner
        StorageItemListLiveData.observe(viewLifecycleOwner) { onStorageItemsChanged(it) }
        QuickAccessItemListLiveData.observe(viewLifecycleOwner) { onQuickAccessItemsChanged(it) }
        BookmarkItemListLiveData.observe(viewLifecycleOwner) { onBookmarkItemsChanged(it) }
        SettingsItemListLiveData.observe(viewLifecycleOwner) { onSettingsItemsChanged(it) }
        listener.observeCurrentPath(viewLifecycleOwner) { onCurrentPathChanged(it) }
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
    }

    private fun onQuickAccessItemsChanged(navigationItems: List<NavigationItem?>) {
        quickAccessListAdapter.replace(navigationItems)
        binding.quickAccessRecyclerView.isVisible = navigationItems.isNotEmpty()
    }

    private fun onBookmarkItemsChanged(navigationItems: List<NavigationItem?>) {
        bookmarkListAdapter.replace(navigationItems)
        binding.bookmarkRecyclerView.isVisible = navigationItems.isNotEmpty()
    }

    private fun onSettingsItemsChanged(navigationItems: List<NavigationItem?>) {
        settingsListAdapter.replace(navigationItems)
    }

    private fun onCurrentPathChanged(path: Path) {
        storageListAdapter.notifyCheckedChanged()
        quickAccessListAdapter.notifyCheckedChanged()
        bookmarkListAdapter.notifyCheckedChanged()
    }

    override val currentPath: Path
        get() = listener.currentPath

    override fun navigateTo(path: Path) {
        listener.navigateTo(path)
    }

    override fun navigateToRoot(path: Path) {
        listener.navigateToRoot(path)
    }

    override fun quickAccessTo(category: MimeTypeCategory?) {
        listener.quickAccessTo(category)
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
        fun navigateTo(path: Path)
        fun navigateToRoot(path: Path)
        fun navigateToDefaultRoot()
        fun quickAccessTo(category: MimeTypeCategory?)
        fun observeCurrentPath(owner: LifecycleOwner, observer: (Path) -> Unit)
        fun closeNavigationDrawer()
    }
}
