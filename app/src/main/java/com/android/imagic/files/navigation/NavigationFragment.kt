package com.android.imagic.files.navigation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.imagic.files.databinding.NewNavigationFragmentBinding
import com.android.imagic.files.filelist.FileListActivity
import com.android.imagic.files.storage.AddStorageDialogActivity
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
    private lateinit var classShortcutListAdapter: ClassShortcutListAdapter
    private lateinit var bookmarkListAdapter: ClassShortcutListAdapter

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
        binding.classShortcutRecyclerView.layoutManager =
            GridLayoutManager(context, 4, GridLayoutManager.VERTICAL, false)
        classShortcutListAdapter = ClassShortcutListAdapter(this, context)
        binding.classShortcutRecyclerView.adapter = classShortcutListAdapter

        binding.bookmarkRecyclerView.layoutManager =
            GridLayoutManager(context, 4, GridLayoutManager.VERTICAL, false)
        bookmarkListAdapter = ClassShortcutListAdapter(this, context)
        binding.bookmarkRecyclerView.adapter = bookmarkListAdapter

        binding.storageRecyclerView.layoutManager = LinearLayoutManager(context)
        storageListAdapter = StorageListAdapter(this, context)
        binding.storageRecyclerView.adapter = storageListAdapter

        binding.loginLayout.setOnClickListener {
            // todo login action
        }
        binding.addStorageLayout.setOnClickListener {
            launchIntent(AddStorageDialogActivity::class.createIntent())
        }

        val viewLifecycleOwner = viewLifecycleOwner
        StorageItemListLiveData.observe(viewLifecycleOwner) { onStorageItemsChanged(it) }
        ClassShortcutItemListLiveData.observe(viewLifecycleOwner) {
            onClassShortcutItemsChanged(it)
            if (classShortcutListAdapter.itemCount == 0) {
                binding.classShortcutRecyclerView.visibility = View.GONE
            }
        }
        BookmarkItemListLiveData.observe(viewLifecycleOwner) { onBookmarkItemsChanged(it) }
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

    private fun onClassShortcutItemsChanged(navigationItems: List<NavigationItem?>) {
        classShortcutListAdapter.replace(navigationItems)
    }

    private fun onBookmarkItemsChanged(navigationItems: List<NavigationItem?>) {
        bookmarkListAdapter.replace(navigationItems)
    }

    private fun onCurrentPathChanged(path: Path) {
        storageListAdapter.notifyCheckedChanged()
        classShortcutListAdapter.notifyCheckedChanged()
    }

    override val currentPath: Path
        get() = listener.currentPath

    override fun navigateTo(path: Path) {
        listener.navigateTo(path)
    }

    override fun navigateToRoot(path: Path) {
        listener.navigateToRoot(path)
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
        fun observeCurrentPath(owner: LifecycleOwner, observer: (Path) -> Unit)
        fun closeNavigationDrawer()
    }
}
