package com.android.imagic.files.filelist

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.DragEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.dispose
import coil.load
import com.android.imagic.files.R
import com.android.imagic.files.app.application
import com.android.imagic.files.app.clipboardManager
import com.android.imagic.files.coil.AppIconPackageName
import com.android.imagic.files.compat.checkSelfPermissionCompat
import com.android.imagic.files.compat.isPrimaryCompat
import com.android.imagic.files.compat.pathCompat
import com.android.imagic.files.compat.setGroupDividerEnabledCompat
import com.android.imagic.files.databinding.FileDragPlaceholderBinding
import com.android.imagic.files.databinding.FileListFragmentAppBarIncludeBinding
import com.android.imagic.files.databinding.FileListFragmentBinding
import com.android.imagic.files.databinding.FileListFragmentBottomBarIncludeBinding
import com.android.imagic.files.databinding.FileListFragmentContentIncludeBinding
import com.android.imagic.files.databinding.FileListFragmentIncludeBinding
import com.android.imagic.files.databinding.FileListFragmentSpeedDialIncludeBinding
import com.android.imagic.files.databinding.FileListFragmentTransferIncludeBinding
import com.android.imagic.files.file.FileItem
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.asMimeTypeOrNull
import com.android.imagic.files.file.cloud.sdrive.CloudFileItem
import com.android.imagic.files.file.extension
import com.android.imagic.files.file.fileProviderUri
import com.android.imagic.files.file.iconRes
import com.android.imagic.files.file.intentType
import com.android.imagic.files.file.isApk
import com.android.imagic.files.file.isImage
import com.android.imagic.files.file.isMedia
import com.android.imagic.files.file.mediaStoreUri
import com.android.imagic.files.filejob.FileJobService
import com.android.imagic.files.filejob.FileJobService.Companion.FILE_TRANSFER_ID
import com.android.imagic.files.filejob.FileJobService.Companion.FILE_TRANSFER_INDETERMINATE
import com.android.imagic.files.filejob.FileJobService.Companion.FILE_TRANSFER_MAX
import com.android.imagic.files.filejob.FileJobService.Companion.FILE_TRANSFER_PROGRESS
import com.android.imagic.files.filejob.FileJobService.Companion.FILE_TRANSFER_TEXT
import com.android.imagic.files.filejob.FileJobService.Companion.FILE_TRANSFER_TITLE
import com.android.imagic.files.fileproperties.CloudFilePropertiesDialogFragment
import com.android.imagic.files.fileproperties.FilePropertiesDialogFragment
import com.android.imagic.files.navigation.BookmarkDirectories
import com.android.imagic.files.navigation.BookmarkDirectory
import com.android.imagic.files.navigation.NavigationFragment
import com.android.imagic.files.navigation.NavigationRootMapLiveData
import com.android.imagic.files.navigation.QuickAccessCategory
import com.android.imagic.files.provider.archive.createArchiveRootPath
import com.android.imagic.files.provider.archive.isArchivePath
import com.android.imagic.files.provider.cloud.sdrive.client.Authority
import com.android.imagic.files.provider.cloud.sdrive.client.exception.LoginExpiredException
import com.android.imagic.files.provider.cloud.sdrive.client.exception.SDriveException
import com.android.imagic.files.provider.common.isDirectory
import com.android.imagic.files.provider.linux.isLinuxPath
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.storage.SDriveServer
import com.android.imagic.files.storage.StorageVolumeListLiveData
import com.android.imagic.files.ui.AppBarLayoutExpandHackListener
import com.android.imagic.files.ui.CheckableForegroundLinearLayout
import com.android.imagic.files.ui.CoordinatorAppBarLayout
import com.android.imagic.files.ui.CrossfadeSubtitleToolbar
import com.android.imagic.files.ui.DrawerLayoutOnBackPressedCallback
import com.android.imagic.files.ui.FileTransferActionMode
import com.android.imagic.files.ui.FileTransferFloatingLayout
import com.android.imagic.files.ui.FixQueryChangeSearchView
import com.android.imagic.files.ui.FloatingButtonView
import com.android.imagic.files.ui.OverlayToolbarActionMode
import com.android.imagic.files.ui.PersistentBarLayout
import com.android.imagic.files.ui.PersistentBarLayoutToolbarActionMode
import com.android.imagic.files.ui.PersistentDrawerLayout
import com.android.imagic.files.ui.ScrollingViewOnApplyWindowInsetsListener
import com.android.imagic.files.ui.SpeedDialViewOnBackPressedCallback
import com.android.imagic.files.ui.ThemedFastScroller
import com.android.imagic.files.ui.ToolbarActionMode
import com.android.imagic.files.util.DebouncedRunnable
import com.android.imagic.files.util.Failure
import com.android.imagic.files.util.Loading
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.Stateful
import com.android.imagic.files.util.Success
import com.android.imagic.files.util.addOnBackPressedCallback
import com.android.imagic.files.util.args
import com.android.imagic.files.util.asFileName
import com.android.imagic.files.util.asFileNameOrNull
import com.android.imagic.files.util.checkSelfPermission
import com.android.imagic.files.util.copyText
import com.android.imagic.files.util.create
import com.android.imagic.files.util.createInstallPackageIntent
import com.android.imagic.files.util.createManageAppAllFilesAccessPermissionIntent
import com.android.imagic.files.util.createSendStreamIntent
import com.android.imagic.files.util.createViewIntent
import com.android.imagic.files.util.extraPath
import com.android.imagic.files.util.extraPathList
import com.android.imagic.files.util.fadeToVisibility
import com.android.imagic.files.util.fadeToVisibilityUnsafe
import com.android.imagic.files.util.finish
import com.android.imagic.files.util.getDimensionDp
import com.android.imagic.files.util.getDrawable
import com.android.imagic.files.util.getQuantityString
import com.android.imagic.files.util.hasSw600Dp
import com.android.imagic.files.util.isMediaPlayerAvailable
import com.android.imagic.files.util.isOrientationLandscape
import com.android.imagic.files.util.setOnEditorConfirmActionListener
import com.android.imagic.files.util.showToast
import com.android.imagic.files.util.startActivitySafe
import com.android.imagic.files.util.supportsExternalStorageManager
import com.android.imagic.files.util.takeIfNotEmpty
import com.android.imagic.files.util.valueCompat
import com.android.imagic.files.util.viewModels
import com.android.imagic.files.util.withChooser
import com.android.imagic.files.viewer.image.ImageViewerActivity
import com.android.imagic.files.viewer.video.VideoPlayerActivity
import com.leinardi.android.speeddial.SpeedDialView
import java8.nio.file.Path
import java8.nio.file.Paths
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import me.zhanghai.android.foregroundcompat.ForegroundImageButton
import kotlin.math.roundToInt

class FileListFragment : Fragment(), BreadcrumbLayout.Listener, FileListAdapter.Listener,
    CloudFileListAdapter.Listener, FileTransferAdapter.Listener,
    ConfirmReplaceFileDialogFragment.Listener, OpenApkDialogFragment.Listener,
    ConfirmDeleteFilesDialogFragment.Listener, ConfirmDeleteCloudFilesDialogFragment.Listener,
    CreateArchiveDialogFragment.Listener, RenameFileDialogFragment.Listener,
    RenameCloudFileDialogFragment.Listener, CreateFileDialogFragment.Listener,
    CreateCloudFileDialogFragment.Listener, CreateDirectoryDialogFragment.Listener,
    CreateCloudDirectoryDialogFragment.Listener, ConfirmDownloadCloudFileDialogFragment.Listener,
    NavigateToPathDialogFragment.Listener, NavigationFragment.Listener,
    ShowRequestAllFilesAccessRationaleDialogFragment.Listener,
    ShowRequestNotificationPermissionRationaleDialogFragment.Listener,
    ShowRequestNotificationPermissionInSettingsRationaleDialogFragment.Listener,
    ShowRequestStoragePermissionRationaleDialogFragment.Listener,
    ShowRequestStoragePermissionInSettingsRationaleDialogFragment.Listener {
    private val requestAllFilesAccessLauncher = registerForActivityResult(
        RequestAllFilesAccessContract(), this::onRequestAllFilesAccessResult
    )
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(), this::onRequestStoragePermissionResult
    )
    private val requestStoragePermissionInSettingsLauncher = registerForActivityResult(
        RequestPermissionInSettingsContract(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
        this::onRequestStoragePermissionInSettingsResult
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(), this::onRequestNotificationPermissionResult
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestNotificationPermissionInSettingsLauncher = registerForActivityResult(
        RequestPermissionInSettingsContract(android.Manifest.permission.POST_NOTIFICATIONS),
        this::onRequestNotificationPermissionInSettingsResult
    )

    private val args by args<Args>()
    private val argsPath by lazy { args.intent.extraPath }

    private val viewModel by viewModels { { FileListViewModel() } }

    private lateinit var binding: Binding

    private lateinit var navigationFragment: NavigationFragment

    private lateinit var menuBinding: MenuBinding

    private lateinit var overlayActionMode: ToolbarActionMode

    private lateinit var bottomActionMode: PersistentBarLayoutToolbarActionMode

    private lateinit var fileTransferActionMode: FileTransferActionMode

    private lateinit var layoutManager: GridLayoutManager

    private lateinit var cloudLayoutManager: GridLayoutManager

    private lateinit var adapter: FileListAdapter

    private lateinit var cloudFileListAdapter: CloudFileListAdapter

    private lateinit var fileTransferAdapter: FileTransferAdapter

    private val handler = Handler(Looper.getMainLooper())

    private val handlerThread = HandlerThread("FileTransferThread").apply { start() }

    private val workerHandler = Handler(handlerThread.looper)

    private val debouncedSearchRunnable = DebouncedRunnable(handler, 1000) {
        if (!isResumed || !viewModel.isSearchViewExpanded) {
            return@DebouncedRunnable
        }
        val query = viewModel.searchViewQuery
        if (query.isEmpty()) {
            return@DebouncedRunnable
        }
        viewModel.search(query)
    }

    private val fileTransferReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            workerHandler.post {
                val acton = intent.action
                val id = intent.getIntExtra(FILE_TRANSFER_ID, -1)
                val fileTransfer = FileTransfer(
                    id = id,
                    title = intent.getStringExtra(FILE_TRANSFER_TITLE),
                    text = intent.getStringExtra(FILE_TRANSFER_TEXT),
                    max = intent.getIntExtra(FILE_TRANSFER_MAX, 100),
                    progress = intent.getIntExtra(FILE_TRANSFER_PROGRESS, 0),
                    indeterminate = intent.getBooleanExtra(FILE_TRANSFER_INDETERMINATE, false),
                    status = if (acton == FileJobService.Actions.CANCELLING.toString()) {
                        FileTransfer.Status.CANCELLING
                    } else FileTransfer.Status.TRANSFERRING
                )

                when (acton) {
                    FileJobService.Actions.STARTED.toString(),
                    FileJobService.Actions.PROGRESS.toString(),
                    FileJobService.Actions.CANCELLING.toString() -> {
                        handler.post { updateTransferProgress(fileTransfer) }
                    }

                    FileJobService.Actions.CANCELLED.toString() -> {
                        handler.post { removeTransferItem(id) }
                    }

                    else -> return@post
                }
            }
        }
    }

    private fun updateTransferProgress(fileTransfer: FileTransfer) {
        fileTransferAdapter.updateItem(fileTransfer)
        if (fileTransferAdapter.list.isNotEmpty()) {
            fileTransferActionMode.show()
            val size = fileTransferAdapter.list.size
            binding.transferTitle.text = getQuantityString(
                R.plurals.file_transferring_title_format, size, size
            )
        }
    }

    private fun removeTransferItem(id: Int) {
        fileTransferAdapter.removeItem(id)
        if (fileTransferAdapter.list.isEmpty()) {
            fileTransferActionMode.hide()
        } else {
            val size = fileTransferAdapter.list.size
            binding.transferTitle.text = getQuantityString(
                R.plurals.file_transferring_title_format, size, size
            )
        }
        refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        lifecycleScope.launchWhenStarted {
            viewModel.fileTransferState.collect { transfer ->
                when (transfer.status) {
                    FileTransfer.Status.TRANSFERRING,
                    FileTransfer.Status.CANCELLING -> updateTransferProgress(transfer)

                    FileTransfer.Status.FINISHED,
                    FileTransfer.Status.FAILED -> removeTransferItem(transfer.id)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = Binding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            navigationFragment = NavigationFragment()
            childFragmentManager.commit { add(R.id.navigationFragment, navigationFragment) }
        } else {
            navigationFragment = childFragmentManager.findFragmentById(R.id.navigationFragment)
                    as NavigationFragment
        }
        navigationFragment.listener = this
        val activity = requireActivity() as AppCompatActivity
        activity.setTitle(R.string.app_name)
        activity.setSupportActionBar(binding.toolbar)
        overlayActionMode = OverlayToolbarActionMode(binding.overlayToolbar)
        bottomActionMode = PersistentBarLayoutToolbarActionMode(
            binding.persistentBarLayout, binding.bottomProgressbar,
            binding.bottomBarLayout, binding.bottomToolbar
        )
        val contentLayoutInitialPaddingBottom = binding.contentLayout.paddingBottom
        binding.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
            binding.contentLayout.updatePaddingRelative(
                bottom = contentLayoutInitialPaddingBottom +
                        binding.appBarLayout.totalScrollRange + verticalOffset
            )
        }
        binding.appBarLayout.syncBackgroundColorTo(binding.overlayToolbar)
        binding.breadcrumbLayout.setListener(this)
        if (!(activity.hasSw600Dp && activity.isOrientationLandscape)) {
            binding.swipeRefreshLayout.setProgressViewEndTarget(
                true, binding.swipeRefreshLayout.progressViewEndOffset
            )
        }
        binding.checkAllLayout.apply {
            setOnClickListener {
                if (isChecked) {
                    clearSelectedFiles()
                } else {
                    selectAllFiles()
                }
            }
        }
        binding.swipeRefreshLayout.setOnRefreshListener { refresh() }
        layoutManager = GridLayoutManager(activity, 1)
        binding.recyclerView.layoutManager = layoutManager
        adapter = FileListAdapter(this)
        binding.recyclerView.adapter = adapter
        val fastScroller = ThemedFastScroller.create(binding.recyclerView)
        binding.recyclerView.setOnApplyWindowInsetsListener(
            ScrollingViewOnApplyWindowInsetsListener(binding.recyclerView, fastScroller)
        )
        binding.recyclerView.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED, DragEvent.ACTION_DRAG_ENTERED,
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED,
                DragEvent.ACTION_DRAG_LOCATION -> {
                    true
                }

                DragEvent.ACTION_DROP -> {
                    val path = currentPath
                    if (path.isDirectory()) {
                        dropToLocal(path)
                    }
                    true
                }

                else -> false
            }
        }

        cloudLayoutManager = GridLayoutManager(activity, 1)
        binding.cloudRecyclerView.layoutManager = cloudLayoutManager
        cloudFileListAdapter = CloudFileListAdapter(this)
        binding.cloudRecyclerView.adapter = cloudFileListAdapter
        val cloudFastScroller = ThemedFastScroller.create(binding.cloudRecyclerView)
        binding.cloudRecyclerView.setOnApplyWindowInsetsListener(
            ScrollingViewOnApplyWindowInsetsListener(binding.cloudRecyclerView, cloudFastScroller)
        )
        binding.cloudRecyclerView.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED, DragEvent.ACTION_DRAG_ENTERED,
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED,
                DragEvent.ACTION_DRAG_LOCATION -> {
                    true
                }

                DragEvent.ACTION_DROP -> {
                    val cloudDirectoryPath = viewModel.currentCloudPath
                        ?: return@setOnDragListener false
                    dropToCloud(cloudDirectoryPath)
                    true
                }

                else -> false
            }
        }
        binding.speedDialView.inflate(R.menu.file_list_speed_dial)
        binding.speedDialView.setOnActionSelectedListener {
            when (it.id) {
                R.id.action_create_file -> showCreateFileDialog()
                R.id.action_create_directory -> showCreateDirectoryDialog()
            }
            // Returning false causes the speed dial to close without animation.
            //return false
            binding.speedDialView.close()
            true
        }
        binding.transferRecyclerView.layoutManager = LinearLayoutManager(context)
        fileTransferAdapter = FileTransferAdapter(this)
        binding.transferRecyclerView.adapter = fileTransferAdapter
        fileTransferActionMode = FileTransferActionMode(
            binding.transferRecyclerLayout, binding.floatingButton
        )
        binding.floatingButton.setOnClickListener { fileTransferActionMode.switchView(true) }
        binding.transferDropDown.setOnClickListener { fileTransferActionMode.switchView(false) }

        val viewLifecycleOwner = viewLifecycleOwner
        addOnBackPressedCallback(
            object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    viewModel.navigateUp()
                }
            }.also { callback ->
                viewModel.breadcrumbLiveData.observe(viewLifecycleOwner) {
                    callback.isEnabled = viewModel.canNavigateUpBreadcrumb
                }
            }
        )
        addOnBackPressedCallback(
            object : OnBackPressedCallback(false) {
                private var lastBackPressedTime = 0L

                override fun handleOnBackPressed() {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBackPressedTime < 2000L) {
                        finish()
                    } else {
                        lastBackPressedTime = currentTime
                        showToast(getString(R.string.press_again_exit))
                    }
                }
            }.also { callback ->
                viewModel.breadcrumbLiveData.observe(viewLifecycleOwner) {
                    callback.isEnabled = !viewModel.canNavigateUpBreadcrumb
                }
            }
        )
        addOnBackPressedCallback(overlayActionMode.onBackPressedCallback)
        addOnBackPressedCallback(SpeedDialViewOnBackPressedCallback(binding.speedDialView))
        binding.drawerLayout?.let {
            addOnBackPressedCallback(DrawerLayoutOnBackPressedCallback(it))
        }

        if (!viewModel.hasTrail) {
            var path = argsPath
            val intent = args.intent
            var pickOptions: PickOptions? = null
            when (val action = intent.action) {
                Intent.ACTION_GET_CONTENT, Intent.ACTION_OPEN_DOCUMENT,
                Intent.ACTION_CREATE_DOCUMENT -> {
                    val mode = if (action == Intent.ACTION_CREATE_DOCUMENT) {
                        PickOptions.Mode.CREATE_FILE
                    } else {
                        PickOptions.Mode.OPEN_FILE
                    }
                    val mimeType = intent.type?.asMimeTypeOrNull() ?: MimeType.ANY
                    val fileName = if (mode == PickOptions.Mode.CREATE_FILE) {
                        intent.getStringExtra(Intent.EXTRA_TITLE)?.asFileNameOrNull()?.value
                            ?: mimeType.extension?.let { "file.$it" } ?: "file"
                    } else {
                        null
                    }
                    val readOnly = action == Intent.ACTION_GET_CONTENT
                    val extraMimeTypes = if (mode == PickOptions.Mode.OPEN_FILE) {
                        intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)
                            ?.mapNotNull { it.asMimeTypeOrNull() }?.takeIfNotEmpty()
                    } else {
                        null
                    }
                    val mimeTypes = extraMimeTypes ?: listOf(mimeType)
                    val localOnly = intent.getBooleanExtra(Intent.EXTRA_LOCAL_ONLY, false)
                    val allowMultiple = mode != PickOptions.Mode.CREATE_FILE &&
                            intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                    pickOptions =
                        PickOptions(mode, fileName, readOnly, mimeTypes, localOnly, allowMultiple)
                }

                Intent.ACTION_OPEN_DOCUMENT_TREE -> {
                    val localOnly = intent.getBooleanExtra(Intent.EXTRA_LOCAL_ONLY, false)
                    pickOptions = PickOptions(
                        PickOptions.Mode.OPEN_DIRECTORY, null, false, emptyList(), localOnly, false
                    )
                }

                ACTION_VIEW_DOWNLOADS ->
                    path = Paths.get(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        ).path
                    )

                else ->
                    if (path != null) {
                        val mimeType = intent.type?.asMimeTypeOrNull()
                        if (mimeType != null && path.isArchiveFile(mimeType)) {
                            path = path.createArchiveRootPath()
                        }
                    }
            }
            if (path == null) {
                path = Settings.FILE_LIST_DEFAULT_DIRECTORY.valueCompat
            }
            viewModel.resetTo(path)
            if (pickOptions != null) {
                viewModel.pickOptions = pickOptions
            }
        }
        viewModel.currentFileModeStateLiveData.observe(viewLifecycleOwner) {
            onFileModeStateChanged(it)
        }
        viewModel.currentPathLiveData.observe(viewLifecycleOwner) { onCurrentPathChanged(it) }
        viewModel.searchViewExpandedLiveData.observe(viewLifecycleOwner) {
            onSearchViewExpandedChanged(it)
        }
        viewModel.breadcrumbLiveData.observe(viewLifecycleOwner) { breadcrumbData ->
            binding.breadcrumbLayout.setData(breadcrumbData)
            breadcrumbData.nameProducers.firstOrNull()?.let { producer ->
                val storageTitle = producer.invoke(requireContext())
                if ("/" == storageTitle) {
                    val internalStoragePath = StorageVolumeListLiveData.valueCompat
                        .find { it.isPrimaryCompat }!!
                        .let { Paths.get(it.pathCompat) }
                    navigateToRoot(internalStoragePath)
                } else if (!activity.title.equals(storageTitle)) {
                    activity.setTitle(storageTitle)
                }
            }
        }
        viewModel.viewTypeLiveData.observe(viewLifecycleOwner) { onViewTypeChanged(it) }
        // Live data only calls observeForever() on its sources when it is active, so we have to
        // make view type live data active first (so that it can load its initial value) before we
        // register another observer that needs to get the view type.
        if (binding.persistentDrawerLayout != null) {
            Settings.FILE_LIST_PERSISTENT_DRAWER_OPEN.observe(viewLifecycleOwner) {
                onPersistentDrawerOpenChanged(it)
            }
        }
        viewModel.sortOptionsLiveData.observe(viewLifecycleOwner) { onSortOptionsChanged(it) }
        viewModel.viewSortPathSpecificLiveData.observe(viewLifecycleOwner) {
            onViewSortPathSpecificChanged(it)
        }
        viewModel.pickOptionsLiveData.observe(viewLifecycleOwner) { onPickOptionsChanged(it) }
        viewModel.selectedFilesLiveData.observe(viewLifecycleOwner) { onSelectedFilesChanged(it) }
        viewModel.selectedCloudFilesLiveData.observe(viewLifecycleOwner) {
            onSelectedCloudFilesChanged(it)
        }
        viewModel.isAllSelectedLiveData.observe(viewLifecycleOwner) { onAllSelectedChanged(it) }
        viewModel.pasteStateLiveData.observe(viewLifecycleOwner) { onPasteStateChanged(it) }
        viewModel.cloudPasteStateLiveData.observe(viewLifecycleOwner) { onCloudPasteStateChanged(it) }
        Settings.FILE_NAME_ELLIPSIZE.observe(viewLifecycleOwner) { onFileNameEllipsizeChanged(it) }
        viewModel.currentFileModeStateLiveData.observe(viewLifecycleOwner) {
            onCurrentFileModeStateChanged(it)
        }
        viewModel.currentCloudPathLiveData.observe(viewLifecycleOwner) { onCloudPathListChanged(it) }
        viewModel.fileListLiveData.observe(viewLifecycleOwner) { onFileListChanged(it) }
        viewModel.cloudFileListLiveData.observe(viewLifecycleOwner) { onCloudFileListChanged(it) }
        viewModel.breadcrumbLiveData.observe(viewLifecycleOwner) {
            binding.toolbar.setNavButtonEnable(viewModel.canNavigateUpBreadcrumb)
        }
        Settings.FILE_LIST_SHOW_HIDDEN_FILES.observe(viewLifecycleOwner) {
            onShowHiddenFilesChanged(it)
        }
    }

    override fun onStart() {
        super.onStart()

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            fileTransferReceiver, IntentFilter().apply {
                addAction(FileJobService.Actions.PROGRESS.toString())
                addAction(FileJobService.Actions.CANCELLED.toString())
                addAction(FileJobService.Actions.FAILED.toString())
                addAction(FileJobService.Actions.FINISHED.toString())
            }
        )
    }

    override fun onResume() {
        super.onResume()

        if (!viewModel.isNotificationPermissionRequested) {
            ensureStorageAccess()
        }
        if (!viewModel.isStorageAccessRequested) {
            ensureNotificationPermission()
        }
    }

    override fun onStop() {
        super.onStop()

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(fileTransferReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menuBinding = MenuBinding.inflate(menu, inflater)
        menuBinding.sortItem.subMenu!!.setGroupDividerEnabledCompat(true)
        setUpSearchView()
        menuBinding.viewTypeItem.setOnMenuItemClickListener {
            viewModel.viewType = when (viewModel.viewType) {
                FileViewType.LIST -> FileViewType.GRID
                FileViewType.GRID -> FileViewType.LIST
            }
            true
        }
    }

    private fun setUpSearchView() {
        val searchView = menuBinding.searchItem.actionView as FixQueryChangeSearchView
        // MenuItem.OnActionExpandListener.onMenuItemActionExpand() is called before SearchView
        // resets the query.
        searchView.setOnSearchClickListener {
            viewModel.isSearchViewExpanded = true
            searchView.setQuery(viewModel.searchViewQuery, false)
            debouncedSearchRunnable()
        }
        // SearchView.OnCloseListener.onClose() is not always called.
        menuBinding.searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.isSearchViewExpanded = false
                viewModel.stopSearching()
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                debouncedSearchRunnable.cancel()
                viewModel.search(query)
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                if (searchView.shouldIgnoreQueryChange) {
                    return false
                }
                viewModel.searchViewQuery = query
                debouncedSearchRunnable()
                return false
            }
        })
        if (viewModel.isSearchViewExpanded) {
            menuBinding.searchItem.expandActionView()
        }
    }

    private fun collapseSearchView() {
        if (this::menuBinding.isInitialized && menuBinding.searchItem.isActionViewExpanded) {
            menuBinding.searchItem.collapseActionView()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        updateViewSortMenuItems()
        updateShowHiddenFilesMenuItem()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (viewModel.isCloudFileMode) {
                    navigateCloudUp()
                } else {
                    navigateUp()
                }
                true
            }

            R.id.action_sort_by_name -> {
                viewModel.setSortBy(By.NAME)
                true
            }

            R.id.action_sort_by_type -> {
                viewModel.setSortBy(By.TYPE)
                true
            }

            R.id.action_sort_by_size -> {
                viewModel.setSortBy(By.SIZE)
                true
            }

            R.id.action_sort_by_last_modified -> {
                viewModel.setSortBy(By.LAST_MODIFIED)
                true
            }

            R.id.action_sort_order_ascending -> {
                viewModel.setSortOrder(
                    if (!menuBinding.sortOrderAscendingItem.isChecked) {
                        Order.ASCENDING
                    } else {
                        Order.DESCENDING
                    }
                )
                true
            }

            R.id.action_sort_directories_first -> {
                viewModel.setSortDirectoriesFirst(!menuBinding.sortDirectoriesFirstItem.isChecked)
                true
            }

            R.id.action_view_sort_path_specific -> {
                viewModel.isViewSortPathSpecific = !menuBinding.viewSortPathSpecificItem.isChecked
                true
            }

            R.id.action_new_task -> {
                newTask()
                true
            }

            R.id.action_navigate_to -> {
                showNavigateToPathDialog()
                true
            }

            R.id.action_refresh -> {
                refresh()
                true
            }

            R.id.action_share -> {
                share()
                true
            }

            R.id.action_copy_path -> {
                copyPath()
                true
            }

            R.id.action_create_file -> {
                if (viewModel.isCloudFileMode) {
                    showCreateCloudFileDialog()
                } else {
                    showCreateFileDialog()
                }
                true
            }

            R.id.action_create_directory -> {
                showCreateDirectoryDialog()
                true
            }

            R.id.action_show_hidden_files -> {
                setShowHiddenFiles(!menuBinding.showHiddenFilesItem.isChecked)
                true
            }

            R.id.action_exit_app -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
        if (bottomActionMode.isActive) {
            val menu = bottomActionMode.menu
            menu.setQwertyMode(
                KeyCharacterMap.load(event.deviceId).keyboardType != KeyCharacterMap.NUMERIC
            )
            if (menu.performShortcut(keyCode, event, 0)) {
                return true
            }
        }
        if (overlayActionMode.isActive) {
            val menu = overlayActionMode.menu
            menu.setQwertyMode(
                KeyCharacterMap.load(event.deviceId).keyboardType != KeyCharacterMap.NUMERIC
            )
            if (menu.performShortcut(keyCode, event, 0)) {
                return true
            }
        }
        return false
    }

    private fun onPersistentDrawerOpenChanged(open: Boolean) {
        binding.persistentDrawerLayout?.let {
            if (open) {
                it.openDrawer(GravityCompat.START)
            } else {
                it.closeDrawer(GravityCompat.START)
            }
        }
        updateSpanCount()
    }

    private fun onFileModeStateChanged(mode: FileModeState) {
        if (mode is FileModeState.Cloud) {
            requireActivity().title = getString(R.string.starboard_drive)
            switchViewWithFade(binding.cloudRecyclerView, binding.recyclerView)
        } else {
            switchViewWithFade(binding.recyclerView, binding.cloudRecyclerView)
        }
    }

    private fun switchViewWithFade(showView: View, hideView: View) = lifecycleScope.launch {
        hideView.fadeToVisibility(false)
        showView.fadeToVisibility(true)
    }

    private fun onCurrentPathChanged(path: Path) {
        updateOverlayToolbar()
        updateBottomToolbar()
    }

    private fun onSearchViewExpandedChanged(expanded: Boolean) {
        updateViewSortMenuItems()
    }

    private fun onCurrentFileModeStateChanged(mode: FileModeState) {
        when (mode) {
            FileModeState.Local -> Unit
            is FileModeState.QuickAccess -> Unit

            is FileModeState.Cloud -> {
                val authority = mode.authority
                viewModel.setCloudAuthority(authority)
                val pathData = CloudPathData(0, authority.username)
                navigateCloudTo(pathData)
            }
        }
    }

    private fun onCloudPathListChanged(pathData: CloudPathData?) {
        if (pathData == null) return
        viewModel.loadCloudFileList(pathData)
    }

    private fun onFileListChanged(stateful: Stateful<List<FileItem>>) {
        val files = stateful.value
        val isSearching = viewModel.searchState.isSearching
        val hasFiles = !files.isNullOrEmpty()
        binding.swipeRefreshLayout.isRefreshing = stateful is Loading && (hasFiles || isSearching)
        binding.progress.fadeToVisibilityUnsafe(stateful is Loading && !(hasFiles || isSearching))
        binding.errorText.fadeToVisibilityUnsafe(stateful is Failure && !hasFiles)
        val throwable = (stateful as? Failure)?.throwable
        if (throwable != null) {
            throwable.printStackTrace()
            if (hasFiles) {
                showToast(R.string.error)
            }
        }
        binding.emptyView.fadeToVisibilityUnsafe(stateful is Success && !hasFiles)
        clearSelectedFiles()
        if (files != null) {
            updateAdapterFileList()
        } else {
            // This resets animation as well.
            adapter.clear()
        }
        if (stateful is Success) {
            viewModel.pendingState?.let { layoutManager.onRestoreInstanceState(it) }
        }
    }

    private fun onCloudFileListChanged(stateful: Stateful<List<CloudFileItem>>) {
        val cloudFiles = stateful.value
        val hasFiles = !cloudFiles.isNullOrEmpty()
        binding.swipeRefreshLayout.isRefreshing = stateful is Loading && hasFiles
        binding.progress.fadeToVisibilityUnsafe(stateful is Loading && !hasFiles)
        binding.errorText.fadeToVisibilityUnsafe(stateful is Failure && !hasFiles)
        val throwable = (stateful as? Failure)?.throwable
        if (throwable != null) {
            throwable.printStackTrace()
            val message = when (throwable) {
                is LoginExpiredException -> {
                    Settings.STORAGES.valueCompat.find { it is SDriveServer }?.let {
                        startActivitySafe(it.createEditIntent())
                    }
                    getString(R.string.login_expired)
                }

                is SDriveException -> throwable.message ?: getString(R.string.server_error_occurred)
                else -> getString(R.string.network_error)
            }
            showToast(message)
        }
        binding.emptyView.fadeToVisibilityUnsafe(stateful is Success && !hasFiles)
        clearSelectedFiles()
        if (cloudFiles != null) {
            updateCloudAdapterFileList()
        } else {
            // This resets animation as well.
            cloudFileListAdapter.clear()
        }
        if (stateful is Success) {
            viewModel.pendingState?.let { cloudLayoutManager.onRestoreInstanceState(it) }
        }
    }

    private fun onViewTypeChanged(viewType: FileViewType) {
        updateSpanCount()
        adapter.viewType = viewType
        cloudFileListAdapter.viewType = viewType
        updateViewSortMenuItems()
    }

    private fun updateSpanCount() {
        layoutManager.spanCount = when (viewModel.viewType) {
            FileViewType.LIST -> 1
            FileViewType.GRID -> {
                var widthDp = resources.configuration.screenWidthDp
                val persistentDrawerLayout = binding.persistentDrawerLayout
                if (persistentDrawerLayout != null &&
                    persistentDrawerLayout.isDrawerOpen(GravityCompat.START)
                ) {
                    widthDp -= getDimensionDp(R.dimen.navigation_max_width).roundToInt()
                }
                (widthDp / 180).coerceAtLeast(2)
            }
        }
        cloudLayoutManager.spanCount = when (viewModel.viewType) {
            FileViewType.LIST -> 1
            FileViewType.GRID -> {
                var widthDp = resources.configuration.screenWidthDp
                val persistentDrawerLayout = binding.persistentDrawerLayout
                if (persistentDrawerLayout != null &&
                    persistentDrawerLayout.isDrawerOpen(GravityCompat.START)
                ) {
                    widthDp -= getDimensionDp(R.dimen.navigation_max_width).roundToInt()
                }
                (widthDp / 180).coerceAtLeast(2)
            }
        }
    }

    private fun onSortOptionsChanged(sortOptions: FileSortOptions) {
        val cloudFileSortOptions = sortOptions.toCloudFileSortOptions()
        adapter.sortOptions = sortOptions
        cloudFileListAdapter.sortOptions = cloudFileSortOptions
        updateViewSortMenuItems()
    }

    private fun onViewSortPathSpecificChanged(pathSpecific: Boolean) {
        updateViewSortMenuItems()
    }

    private fun updateViewSortMenuItems() {
        if (!this::menuBinding.isInitialized) {
            return
        }
        val searchViewExpanded = viewModel.isSearchViewExpanded
        menuBinding.sortItem.isVisible = !searchViewExpanded
        if (searchViewExpanded) {
            return
        }
        val viewType = viewModel.viewType
        menuBinding.viewTypeItem.icon = when (viewType) {
            FileViewType.LIST -> getDrawable(R.drawable.view_type_grid_icon)
            FileViewType.GRID -> getDrawable(R.drawable.view_type_list_icon)
        }
        menuBinding.viewTypeItem.title = when (viewType) {
            FileViewType.LIST -> getString(R.string.file_list_action_view_type_grid)
            FileViewType.GRID -> getString(R.string.file_list_action_view_type_list)
        }
        val sortOptions = viewModel.sortOptions
        val checkedSortByItem = when (sortOptions.by) {
            By.NAME -> menuBinding.sortByNameItem
            By.TYPE -> menuBinding.sortByTypeItem
            By.SIZE -> menuBinding.sortBySizeItem
            By.LAST_MODIFIED -> menuBinding.sortByLastModifiedItem
        }
        checkedSortByItem.isChecked = true
        menuBinding.sortOrderAscendingItem.isChecked = sortOptions.order == Order.ASCENDING
        menuBinding.sortDirectoriesFirstItem.isChecked = sortOptions.isDirectoriesFirst
        menuBinding.viewSortPathSpecificItem.isChecked = viewModel.isViewSortPathSpecific
    }

    private fun navigateUp() {
        collapseSearchView()
        viewModel.navigateUp()
    }

    private fun navigateCloudUp() {
        collapseSearchView()
        viewModel.navigateCloudUp()
    }

    private fun showNavigateToPathDialog() {
        NavigateToPathDialogFragment.show(currentPath, this)
    }

    private fun newTask() {
        openInNewTask(currentPath)
    }

    private fun refresh() {
        if (viewModel.isCloudFileMode) {
            viewModel.refreshCloud()
        } else {
            viewModel.reload()
        }
    }

    private fun setShowHiddenFiles(showHiddenFiles: Boolean) {
        Settings.FILE_LIST_SHOW_HIDDEN_FILES.putValue(showHiddenFiles)
    }

    private fun onShowHiddenFilesChanged(showHiddenFiles: Boolean) {
        viewModel.reload()
        updateShowHiddenFilesMenuItem()
    }

    private fun updateAdapterFileList() {
        var files = viewModel.fileListStateful.value ?: return
        val isShowHiddenFiles = Settings.FILE_LIST_SHOW_HIDDEN_FILES.valueCompat
        if (!isShowHiddenFiles) {
            files = files.filterNot { it.isHidden }
        }
        adapter.replaceListAndIsSearching(
            files,
            viewModel.searchState.isSearching,
            isShowHiddenFiles
        )
    }

    private fun updateCloudAdapterFileList() {
        var files = viewModel.cloudFileListStateful.value ?: return
        cloudFileListAdapter.replace(files, false)
    }

    private fun updateShowHiddenFilesMenuItem() {
        if (!this::menuBinding.isInitialized) {
            return
        }
        val showHiddenFiles = Settings.FILE_LIST_SHOW_HIDDEN_FILES.valueCompat
        menuBinding.showHiddenFilesItem.isChecked = showHiddenFiles
    }

    private fun share() {
        shareFile(currentPath, MimeType.DIRECTORY)
    }

    private fun copyPath() {
        copyPath(currentPath)
    }

    override fun navigateTo(path: Path) {
        collapseSearchView()
        val state = layoutManager.onSaveInstanceState()
        viewModel.navigateTo(state!!, path)
    }

    override fun navigateCloudTo(cloudPathData: CloudPathData) {
        collapseSearchView()
        viewModel.navigateCloudTo(cloudPathData)
    }

    override fun copyPath(path: Path) {
        clipboardManager.copyText(path.toUserFriendlyString(), requireContext())
    }

    override fun openInNewTask(path: Path) {
        val intent = FileListActivity.createViewIntent(path)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                        or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
            )
        startActivitySafe(intent)
    }

    private fun onPickOptionsChanged(pickOptions: PickOptions?) {
        val title = if (pickOptions == null) {
            getString(R.string.file_list_title)
        } else {
            val count = if (pickOptions.allowMultiple) Int.MAX_VALUE else 1
            when (pickOptions.mode) {
                PickOptions.Mode.OPEN_FILE ->
                    getQuantityString(R.plurals.file_list_title_open_file, count)

                PickOptions.Mode.CREATE_FILE -> getString(R.string.file_list_title_create_file)
                PickOptions.Mode.OPEN_DIRECTORY ->
                    getQuantityString(R.plurals.file_list_title_open_directory, count)
            }
        }
        requireActivity().title = title
        updateOverlayToolbar()
        updateBottomToolbar()
        adapter.pickOptions = pickOptions
        cloudFileListAdapter.pickOptions = pickOptions
    }

    private fun pickFiles(files: FileItemSet) {
        pickPaths(files.mapTo(linkedSetOf()) { it.path })
    }

    private fun pickPaths(paths: LinkedHashSet<Path>) {
        val intent = Intent().apply {
            val pickOptions = viewModel.pickOptions!!
            if (paths.size == 1) {
                val path = paths.single()
                data = path.fileProviderUri
                extraPath = path
            } else {
                val mimeTypes = pickOptions.mimeTypes.map { it.value }
                val items = paths.map { ClipData.Item(it.fileProviderUri) }
                clipData = ClipData::class.create(null, mimeTypes, items)
                extraPathList = paths.toList()
            }
            var flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (!pickOptions.readOnly) {
                flags = flags or (Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            if (pickOptions.mode == PickOptions.Mode.OPEN_DIRECTORY) {
                flags = flags or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            }
            addFlags(flags)
        }
        requireActivity().run {
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun onSelectedFilesChanged(files: FileItemSet) {
        updateOverlayToolbar()
        adapter.replaceSelectedFiles(files)
    }

    private fun onSelectedCloudFilesChanged(files: CloudFileItemSet) {
        updateOverlayToolbar()
        cloudFileListAdapter.replaceSelectedFiles(files)
    }

    private fun onAllSelectedChanged(isChecked: Boolean) {
        binding.checkAllLayout.isChecked = isChecked
    }

    private fun updateOverlayToolbar() {
        val isInCloudStorage = viewModel.isCloudFileMode
        val files = viewModel.selectedFiles
        val cloudFiles = viewModel.selectedCloudFiles
        if (files.isEmpty() && cloudFiles.isEmpty()) {
            if (overlayActionMode.isActive) {
                overlayActionMode.finish()
            }
            return
        }

        val pickOptions = viewModel.pickOptions
        if (pickOptions != null) {
            overlayActionMode.title = getString(
                R.string.file_list_select_title_format_new,
                if (isInCloudStorage) cloudFiles.size else files.size
            )
            overlayActionMode.setMenuResource(R.menu.file_list_pick)
            val menu = overlayActionMode.menu
            val isOpen = when (pickOptions.mode) {
                PickOptions.Mode.OPEN_FILE, PickOptions.Mode.OPEN_DIRECTORY -> true
                PickOptions.Mode.CREATE_FILE -> false
            }
            menu.findItem(R.id.action_open).isVisible = isOpen
            menu.findItem(R.id.action_create).isVisible = !isOpen
        } else {
            overlayActionMode.title = getString(
                R.string.file_list_select_title_format_new,
                if (isInCloudStorage) cloudFiles.size else files.size
            )
            overlayActionMode.setMenuResource(R.menu.file_list_select)
            val menu = overlayActionMode.menu
            val isAnyFileReadOnly = if (isInCloudStorage) false
            else files.any { it.path.fileSystem.isReadOnly }
            menu.findItem(R.id.action_cut).isVisible = !isAnyFileReadOnly
            val areAllFilesArchivePaths = if (isInCloudStorage) false
            else files.all { it.path.isArchivePath }
            menu.findItem(R.id.action_copy)
                .setIcon(
                    if (areAllFilesArchivePaths) {
                        R.drawable.extract_icon_control_normal_24dp
                    } else {
                        R.drawable.copy_icon_control_normal_24dp
                    }
                )
                .setTitle(
                    if (areAllFilesArchivePaths) {
                        R.string.file_list_select_action_extract
                    } else {
                        R.string.copy
                    }
                )
            menu.findItem(R.id.action_delete).isVisible = !isAnyFileReadOnly
            val areAllFilesArchiveFiles = if (isInCloudStorage) false
            else files.all { it.isArchiveFile }
            menu.findItem(R.id.action_extract).isVisible = areAllFilesArchiveFiles
            val isCurrentPathReadOnly = if (isInCloudStorage) false
            else viewModel.currentPath.fileSystem.isReadOnly
            menu.findItem(R.id.action_archive).isVisible = !isCurrentPathReadOnly
        }
        if (!overlayActionMode.isActive) {
            binding.appBarLayout.setExpanded(true)
            binding.appBarLayout.addOnOffsetChangedListener(
                AppBarLayoutExpandHackListener(binding.recyclerView)
            )
            overlayActionMode.start(object : ToolbarActionMode.Callback {
                override fun onToolbarActionModeMenuItemClicked(
                    toolbarActionMode: ToolbarActionMode,
                    item: MenuItem
                ): Boolean = onOverlayActionModeMenuItemClicked(item)

                override fun onToolbarActionModeFinished(toolbarActionMode: ToolbarActionMode) {
                    onOverlayActionModeFinished()
                }
            })
        }
    }

    private fun onOverlayActionModeMenuItemClicked(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_open -> {
                pickFiles(viewModel.selectedFiles)
                true
            }

            R.id.action_create -> {
                confirmReplaceFile(viewModel.selectedFiles.single())
                true
            }

            R.id.action_cut -> {
                cutCloudFiles(viewModel.selectedCloudFiles)
                cutFiles(viewModel.selectedFiles)
                true
            }

            R.id.action_copy -> {
                if (viewModel.selectedCloudFiles.isNotEmpty()) {
                    copyCloudFiles(viewModel.selectedCloudFiles)
                }
                if (viewModel.selectedFiles.isNotEmpty()) {
                    copyFiles(viewModel.selectedFiles)
                }
                true
            }

            R.id.action_delete -> {
                if (viewModel.isCloudFileMode) {
                    confirmDeleteCloudFiles(viewModel.selectedCloudFiles)
                } else {
                    confirmDeleteFiles(viewModel.selectedFiles)
                }
                true
            }

            R.id.action_extract -> {
                extractFiles(viewModel.selectedFiles)
                true
            }

            R.id.action_archive -> {
                showCreateArchiveDialog(viewModel.selectedFiles)
                true
            }

            R.id.action_share -> {
                shareFiles(viewModel.selectedFiles)
                true
            }

            else -> false
        }

    private fun onOverlayActionModeFinished() {
        viewModel.clearSelectedFiles()
        viewModel.clearSelectedCloudFiles()
        adapter.notifyStateChanged()
        cloudFileListAdapter.notifyStateChanged()
    }

    private fun confirmReplaceFile(file: FileItem, setFileName: Boolean = true) {
        if (setFileName) {
            val fileName = file.name
            binding.bottomCreateFileNameEdit.setText(fileName)
            binding.bottomCreateFileNameEdit.setSelection(
                0, fileName.asFileName().baseName.length
            )
        }
        ConfirmReplaceFileDialogFragment.show(file, this)
    }

    override fun replaceFile(file: FileItem) {
        pickFiles(fileItemSetOf(file))
    }

    private fun cutFiles(files: FileItemSet) {
        viewModel.addToPasteState(false, files)
        viewModel.selectFiles(files, false)
    }

    private fun copyFiles(files: FileItemSet) {
        viewModel.addToPasteState(true, files)
        viewModel.selectFiles(files, false)
    }

    private fun confirmDeleteFiles(files: FileItemSet) {
        ConfirmDeleteFilesDialogFragment.show(files, this)
    }

    override fun deleteFiles(files: FileItemSet) {
        FileJobService.delete(makePathListForJob(files), requireContext())
        viewModel.selectFiles(files, false)
    }

    private fun extractFiles(files: FileItemSet) {
        copyFiles(files.mapTo(fileItemSetOf()) { it.createDummyArchiveRoot() })
        viewModel.selectFiles(files, false)
    }

    private fun showCreateArchiveDialog(files: FileItemSet) {
        CreateArchiveDialogFragment.show(files, this)
    }

    override fun archive(
        files: FileItemSet,
        name: String,
        format: Int,
        filter: Int,
        password: String?
    ) {
        val archiveFile = viewModel.currentPath.resolve(name)
        FileJobService.archive(
            makePathListForJob(files), archiveFile, format, filter, password, requireContext()
        )
        viewModel.selectFiles(files, false)
    }

    private fun shareFiles(files: FileItemSet) {
        shareFiles(files.map { it.path }, files.map { it.mimeType })
        viewModel.selectFiles(files, false)
    }

    private fun selectAllFiles() {
        val isInCloudStorage = viewModel.isCloudFileMode
        if (isInCloudStorage) {
            cloudFileListAdapter.selectAllFiles()
        } else {
            adapter.selectAllFiles()
        }
    }

    private fun onPasteStateChanged(pasteState: PasteState) {
        updateBottomToolbar()
    }

    private fun onCloudPasteStateChanged(cloudPasteState: CloudPasteState) {
        updateBottomToolbar()
    }

    private fun updateBottomToolbar() {
        val pickOptions = viewModel.pickOptions
        if (pickOptions != null) {
            bottomActionMode.setMenuResource(R.menu.file_list_pick_bottom)
            val menu = bottomActionMode.menu
            when (pickOptions.mode) {
                PickOptions.Mode.CREATE_FILE -> {
                    bottomActionMode.title = null
                    binding.bottomCreateFileNameEdit.isVisible = true
                    val createMenuItem = menu.findItem(R.id.action_create)
                    binding.bottomCreateFileNameEdit.setOnEditorConfirmActionListener {
                        onBottomActionModeMenuItemClicked(createMenuItem)
                    }
                    if (!viewModel.isCreateFileNameEditInitialized) {
                        val fileName = pickOptions.fileName!!
                        binding.bottomCreateFileNameEdit.setText(fileName)
                        binding.bottomCreateFileNameEdit.setSelection(
                            0, fileName.asFileName().baseName.length
                        )
                        binding.bottomCreateFileNameEdit.requestFocus()
                        viewModel.isCreateFileNameEditInitialized = true
                    }
                    menu.findItem(R.id.action_open).isVisible = false
                    createMenuItem.isVisible = true
                }

                PickOptions.Mode.OPEN_DIRECTORY -> {
                    val path = viewModel.currentPath
                    val navigationRoot = NavigationRootMapLiveData.valueCompat[path]
                    val name = navigationRoot?.getName(requireContext()) ?: path.name
                    bottomActionMode.title =
                        getString(R.string.file_list_open_current_directory_format, name)
                    binding.bottomCreateFileNameEdit.isVisible = false
                    menu.findItem(R.id.action_open).isVisible = true
                    menu.findItem(R.id.action_create).isVisible = false
                }

                else -> {
                    if (bottomActionMode.isActive) {
                        bottomActionMode.finish()
                    }
                    return
                }
            }
        } else {
            val pasteState = viewModel.pasteState
            val cloudPasteState = viewModel.cloudPasteState
            val files = pasteState.files
            val cloudFiles = cloudPasteState.files
            if (files.isEmpty() && cloudFiles.isEmpty()) {
                if (bottomActionMode.isActive) {
                    bottomActionMode.finish()
                }
                return
            }
            val areAllFilesArchivePaths =
                cloudFiles.isEmpty() && files.all { it.path.isArchivePath }
            val fileSize = cloudFiles.size + files.size
            bottomActionMode.title = getQuantityString(
                if (pasteState.copy || cloudPasteState.copy) {
                    if (areAllFilesArchivePaths) {
                        R.plurals.file_list_paste_extract_title_format_new
                    } else {
                        R.plurals.file_list_paste_copy_title_format_new
                    }
                } else {
                    R.plurals.file_list_paste_move_title_format_new
                }, fileSize, fileSize
            )
            binding.bottomCreateFileNameEdit.isVisible = false
            bottomActionMode.setMenuResource(R.menu.file_list_paste)
            val isCurrentPathReadOnly =
                viewModel.currentPath.fileSystem.isReadOnly && cloudFiles.isEmpty()
            bottomActionMode.menu.findItem(R.id.action_paste)
                .setTitle(
                    if (areAllFilesArchivePaths) R.string.file_list_paste_action_extract_here else R.string.paste
                )
                .isEnabled = !isCurrentPathReadOnly || cloudFiles.isNotEmpty()
        }
        if (!bottomActionMode.isActive) {
            bottomActionMode.start(object : ToolbarActionMode.Callback {
                override fun onToolbarNavigationIconClicked(toolbarActionMode: ToolbarActionMode) {
                    onBottomToolbarNavigationIconClicked()
                }

                override fun onToolbarActionModeMenuItemClicked(
                    toolbarActionMode: ToolbarActionMode,
                    item: MenuItem
                ): Boolean = onBottomActionModeMenuItemClicked(item)

                override fun onToolbarActionModeFinished(toolbarActionMode: ToolbarActionMode) {
                    onBottomActionModeFinished()
                }
            })
        }
    }

    private fun onBottomToolbarNavigationIconClicked() {
        val pickOptions = viewModel.pickOptions
        if (pickOptions != null) {
            requireActivity().finish()
        } else {
            bottomActionMode.finish()
        }
    }

    private fun onBottomActionModeMenuItemClicked(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_open -> {
                pickPaths(linkedSetOf(viewModel.currentPath))
                true
            }

            R.id.action_create -> {
                val fileName = binding.bottomCreateFileNameEdit.text.toString()
                if (fileName.isEmpty()) {
                    showToast(R.string.file_list_create_file_name_error_empty)
                } else if (fileName.asFileNameOrNull() == null) {
                    showToast(R.string.file_list_create_file_name_error_invalid)
                } else {
                    val file = getFileWithName(fileName)
                    if (file != null) {
                        confirmReplaceFile(file, false)
                    } else {
                        val path = viewModel.currentPath.resolve(fileName)
                        pickPaths(linkedSetOf(path))
                    }
                }
                true
            }

            R.id.action_paste -> {
                if (viewModel.isCloudFileMode && viewModel.currentCloudPath != null) {
                    pasteFiles(cloudDirectoryPath = viewModel.currentCloudPath)
                } else {
                    pasteFiles(localDirectory = currentPath)
                }
                true
            }

            else -> false
        }

    private fun onBottomActionModeFinished() {
        val pickOptions = viewModel.pickOptions
        if (pickOptions == null) {
            viewModel.clearPasteState()
            viewModel.clearCloudPasteState()
        }
    }

    private fun pasteFiles(
        localDirectory: Path? = null,
        cloudDirectoryPath: CloudPathData? = null
    ) {
        Log.d(
            "chengluo",
            "pasteFiles localDirectory: $localDirectory cloudDirectoryPath: $cloudDirectoryPath"
        )
        val context = requireContext()
        if (localDirectory != null) {
            viewModel.pasteToLocal(localDirectory, context)
        }
        if (cloudDirectoryPath != null) {
            viewModel.pasteToCloud(cloudDirectoryPath, context)
        }
    }

    private fun makePathListForJob(files: FileItemSet): List<Path> =
        files.map { it.path }.sortedBy { it.toUri() }

    private fun onFileNameEllipsizeChanged(fileNameEllipsize: TextUtils.TruncateAt) {
        adapter.nameEllipsize = fileNameEllipsize
        cloudFileListAdapter.nameEllipsize = fileNameEllipsize
    }

    override fun cancelDragDrop() {
        viewModel.clearPasteState()
        binding.dragPlaceholder.fadeToVisibilityUnsafe(false)
    }

    override fun dropToLocal(directoryPath: Path) {
        Log.d("chengluo", "dropToLocal directoryPath: $directoryPath")
        pasteFiles(directoryPath)
        binding.dragPlaceholder.fadeToVisibilityUnsafe(false)
    }

    override fun dropToCloud(
        cloudDirectoryPath: CloudPathData,
        authority: Authority
    ) {
        Log.d("chengluo", "dropToCloud cloudDirectoryPath: $cloudDirectoryPath")
        viewModel.setCloudAuthority(authority)
        pasteFiles(cloudDirectoryPath = cloudDirectoryPath)
        binding.dragPlaceholder.fadeToVisibilityUnsafe(false)
    }

    override fun getDragShadow(file: FileItem): View {
        binding.iconImage.apply {
            isVisible = true
            val iconRes = file.mimeType.iconRes
            setImageResource(iconRes)
        }
        binding.thumbnailImage.apply {
            dispose()
            setImageDrawable(null)
            val path = file.path
            val attributes = file.attributes
            val supportsThumbnail = file.supportsThumbnail
            isVisible = supportsThumbnail
            if (supportsThumbnail) {
                load(path to attributes)
            }
        }
        binding.appIconBadgeImage.apply {
            dispose()
            setImageDrawable(null)
            val appDirectoryPackageName = file.appDirectoryPackageName
            val hasAppIconBadge = appDirectoryPackageName != null
            isVisible = hasAppIconBadge
            if (hasAppIconBadge) {
                load(AppIconPackageName(appDirectoryPackageName))
            }
        }
        return binding.dragPlaceholder.apply { isVisible = true }
    }

    override fun clearSelectedFiles() {
        viewModel.clearSelectedFiles()
        viewModel.clearSelectedCloudFiles()
    }

    override fun selectFile(file: FileItem, selected: Boolean) {
        viewModel.selectFile(file, selected)
    }

    override fun selectFiles(files: FileItemSet, selected: Boolean) {
        viewModel.selectFiles(files, selected)
    }

    override fun openFile(file: FileItem) {
        val pickOptions = viewModel.pickOptions
        if (pickOptions != null) {
            if (file.attributes.isDirectory) {
                navigateTo(file.path)
            } else {
                when (pickOptions.mode) {
                    PickOptions.Mode.OPEN_FILE -> pickFiles(fileItemSetOf(file))
                    PickOptions.Mode.CREATE_FILE -> confirmReplaceFile(file)
                    PickOptions.Mode.OPEN_DIRECTORY -> {}
                }
            }
            return
        }
        if (file.mimeType.isApk) {
            openApk(file)
            return
        }
        if (file.isListable) {
            navigateTo(file.listablePath)
            return
        }
        openFileWithIntent(file, false)
    }

    private fun openApk(file: FileItem) {
        if (!file.isListable) {
            installApk(file)
            return
        }
        when (Settings.OPEN_APK_DEFAULT_ACTION.valueCompat) {
            OpenApkDefaultAction.INSTALL -> installApk(file)
            OpenApkDefaultAction.VIEW -> viewApk(file)
            OpenApkDefaultAction.ASK -> OpenApkDialogFragment.show(file, this)
        }
    }

    override fun installApk(file: FileItem) {
        val path = file.path
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!path.isArchivePath) path.fileProviderUri else null
        } else {
            // PackageInstaller only supports file URI before N.
            if (path.isLinuxPath) Uri.fromFile(path.toFile()) else null
        }
        if (uri != null) {
            startActivitySafe(uri.createInstallPackageIntent())
        } else {
            FileJobService.installApk(path, requireContext())
        }
    }

    override fun viewApk(file: FileItem) {
        navigateTo(file.listablePath)
    }

    override fun openFileWith(file: FileItem) {
        openFileWithIntent(file, true)
    }

    private fun openFileWithIntent(file: FileItem, withChooser: Boolean) {
        val path = file.path
        val mimeType = file.mimeType
        if (path.isArchivePath) {
            FileJobService.open(path, mimeType, withChooser, requireContext())
        } else {
            val uri = if (isMediaPlayerAvailable() && mimeType.isMedia) {
                path.mediaStoreUri(requireContext()) ?: path.fileProviderUri
            } else {
                path.fileProviderUri
            }
            val intent = uri.createViewIntent(mimeType)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                .apply {
                    extraPath = path
                    maybeAddImageViewerActivityExtras(this, path, mimeType)
                }
                .let { if (withChooser) it.withChooser() else it }
            startActivitySafe(intent)
        }
    }

    private fun maybeAddImageViewerActivityExtras(intent: Intent, path: Path, mimeType: MimeType) {
        if (!mimeType.isImage) {
            return
        }
        var paths = mutableListOf<Path>()
        // We need the ordered list from our adapter instead of the list from FileListLiveData.
        for (index in 0..<adapter.itemCount) {
            val file = adapter.getItem(index)
            val filePath = file.path
            if (file.mimeType.isImage || filePath == path) {
                paths.add(filePath)
            }
        }
        var position = paths.indexOf(path)
        if (position == -1) {
            return
        }
        // HACK: Don't send too many paths to avoid TransactionTooLargeException.
        if (paths.size > IMAGE_VIEWER_ACTIVITY_PATH_LIST_SIZE_MAX) {
            val start = (position - IMAGE_VIEWER_ACTIVITY_PATH_LIST_SIZE_MAX / 2)
                .coerceIn(0, paths.size - IMAGE_VIEWER_ACTIVITY_PATH_LIST_SIZE_MAX)
            paths = paths.subList(start, start + IMAGE_VIEWER_ACTIVITY_PATH_LIST_SIZE_MAX)
            position -= start
        }
        ImageViewerActivity.putExtras(intent, paths, position)
    }

    override fun cutFile(file: FileItem) {
        cutFiles(fileItemSetOf(file))
    }

    override fun copyFile(file: FileItem) {
        copyFiles(fileItemSetOf(file))
    }

    override fun confirmDeleteFile(file: FileItem) {
        confirmDeleteFiles(fileItemSetOf(file))
    }

    override fun showRenameFileDialog(file: FileItem) {
        RenameFileDialogFragment.show(file, this)
    }

    override fun hasFileWithName(name: String): Boolean = getFileWithName(name) != null

    private fun getFileWithName(name: String): FileItem? {
        val fileListData = viewModel.fileListStateful
        if (fileListData !is Success) {
            return null
        }
        return fileListData.value.find { it.name == name }
    }

    override fun renameFile(file: FileItem, newName: String) {
        FileJobService.rename(file.path, newName, requireContext())
        viewModel.selectFile(file, false)
    }

    override fun extractFile(file: FileItem) {
        copyFile(file.createDummyArchiveRoot())
    }

    override fun showCreateArchiveDialog(file: FileItem) {
        showCreateArchiveDialog(fileItemSetOf(file))
    }

    override fun shareFile(file: FileItem) {
        shareFile(file.path, file.mimeType)
    }

    private fun shareFile(path: Path, mimeType: MimeType) {
        shareFiles(listOf(path), listOf(mimeType))
    }

    private fun shareFiles(paths: List<Path>, mimeTypes: List<MimeType>) {
        val uris = paths.map { it.fileProviderUri }
        val intent = uris.createSendStreamIntent(mimeTypes)
            .withChooser()
        startActivitySafe(intent)
    }

    override fun copyPath(file: FileItem) {
        copyPath(file.path)
    }

    override fun addBookmark(file: FileItem) {
        addBookmark(file.path)
    }

    private fun addBookmark() {
        addBookmark(currentPath)
    }

    private fun addBookmark(path: Path) {
        BookmarkDirectories.add(BookmarkDirectory(null, path))
        showToast(R.string.file_add_bookmark_success)
    }

    override fun createShortcut(file: FileItem) {
        createShortcut(file.path, file.mimeType)
    }

    private fun createShortcut() {
        createShortcut(currentPath, MimeType.DIRECTORY)
    }

    private fun createShortcut(path: Path, mimeType: MimeType) {
        val context = requireContext()
        val isDirectory = mimeType == MimeType.DIRECTORY
        val shortcutInfo = ShortcutInfoCompat.Builder(context, path.toString())
            .setShortLabel(path.name)
            .setIntent(
                if (isDirectory) {
                    FileListActivity.createViewIntent(path)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                } else {
                    OpenFileActivity.createIntent(path, mimeType)
                }
            )
            .setIcon(
                IconCompat.createWithResource(
                    context, if (isDirectory) {
                        R.mipmap.directory_shortcut_icon
                    } else {
                        R.mipmap.file_shortcut_icon
                    }
                )
            )
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            showToast(R.string.shortcut_created)
        }
    }

    override fun showPropertiesDialog(file: FileItem) {
        FilePropertiesDialogFragment.show(file, this)
    }

    private fun showCreateFileDialog() {
        CreateFileDialogFragment.show(this)
    }

    private fun showCreateCloudFileDialog() {
        CreateCloudFileDialogFragment.show(this)
    }

    override fun createFile(name: String) {
        val path = currentPath.resolve(name)
        FileJobService.create(path, false, requireContext())
    }

    private fun showCreateDirectoryDialog() {
        if (viewModel.isCloudFileMode) {
            CreateCloudDirectoryDialogFragment.show(this)
        } else {
            CreateDirectoryDialogFragment.show(this)
        }
    }

    override fun createDirectory(name: String) {
        val path = currentPath.resolve(name)
        FileJobService.create(path, true, requireContext())
    }

    override val currentPath: Path
        get() = viewModel.currentPath

    override val currentFileMode: FileModeState
        get() = viewModel.currentFileMode

    override fun setFileModeState(mode: FileModeState) {
        viewModel.setFileModeState(mode)
    }

    override fun navigateToRoot(path: Path) {
        collapseSearchView()
        viewModel.resetTo(path)
        setFileModeState(FileModeState.Local)
    }

    override fun navigateToDefaultRoot() {
        navigateToRoot(Settings.FILE_LIST_DEFAULT_DIRECTORY.valueCompat)
    }

    override fun observeCurrentPath(owner: LifecycleOwner, observer: (Path) -> Unit) {
        viewModel.currentPathLiveData.observe(owner, observer)
    }

    override fun observeCurrentQuickAccessCategory(
        owner: LifecycleOwner,
        observer: (QuickAccessCategory?) -> Unit
    ) {
        viewModel.currentFileModeStateLiveData.observe(owner) { fileModeState ->
            if (fileModeState is FileModeState.QuickAccess) {
                observer(fileModeState.quickAccessWrap.quickAccess.category)
            } else {
                observer(null)
            }
        }
    }

    override fun closeNavigationDrawer() {
        binding.drawerLayout?.closeDrawer(GravityCompat.START)
    }

    private fun ensureStorageAccess() {
        if (viewModel.isStorageAccessRequested) {
            return
        }
        if (Environment::class.supportsExternalStorageManager()) {
            if (!Environment.isExternalStorageManager()) {
                ShowRequestAllFilesAccessRationaleDialogFragment.show(this)
                viewModel.isStorageAccessRequested = true
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                if (shouldShowRequestPermissionRationale(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    ShowRequestStoragePermissionRationaleDialogFragment.show(this)
                } else {
                    requestStoragePermission()
                }
                viewModel.isStorageAccessRequested = true
            }
        }
    }

    override fun onShowRequestAllFilesAccessRationaleResult(shouldRequest: Boolean) {
        if (shouldRequest) {
            requestAllFilesAccess()
        } else {
            viewModel.isStorageAccessRequested = false
            // This isn't an onActivityResult() callback so it's not delivered before calling
            // onResume(), and we need to do this manually.
            ensureNotificationPermission()
        }
    }

    private fun requestAllFilesAccess() {
        requestAllFilesAccessLauncher.launch(Unit)
    }

    private fun onRequestAllFilesAccessResult(isGranted: Boolean) {
        viewModel.isStorageAccessRequested = false
        if (isGranted) {
            refresh()
        }
    }

    override fun onShowRequestStoragePermissionRationaleResult(shouldRequest: Boolean) {
        if (shouldRequest) {
            requestStoragePermission()
        } else {
            viewModel.isStorageAccessRequested = false
        }
    }

    private fun requestStoragePermission() {
        requestStoragePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun onRequestStoragePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            viewModel.isStorageAccessRequested = false
            refresh()
        } else if (shouldShowRequestPermissionRationale(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            ShowRequestStoragePermissionRationaleDialogFragment.show(this)
        } else {
            ShowRequestStoragePermissionInSettingsRationaleDialogFragment.show(this)
        }
    }

    override fun onShowRequestStoragePermissionInSettingsRationaleResult(shouldRequest: Boolean) {
        if (shouldRequest) {
            requestStoragePermissionInSettings()
        } else {
            viewModel.isStorageAccessRequested = false
        }
    }

    private fun requestStoragePermissionInSettings() {
        requestStoragePermissionInSettingsLauncher.launch(Unit)
    }

    private fun onRequestStoragePermissionInSettingsResult(isGranted: Boolean) {
        viewModel.isStorageAccessRequested = false
        if (isGranted) {
            refresh()
        }
    }

    private fun ensureNotificationPermission() {
        if (viewModel.isNotificationPermissionRequested) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                if (shouldShowRequestPermissionRationale(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                ) {
                    ShowRequestNotificationPermissionRationaleDialogFragment.show(this)
                } else {
                    requestNotificationPermission()
                }
                viewModel.isNotificationPermissionRequested = true
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onShowRequestNotificationPermissionRationaleResult(shouldRequest: Boolean) {
        if (shouldRequest) {
            requestNotificationPermission()
        } else {
            viewModel.isNotificationPermissionRequested = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun onRequestNotificationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            viewModel.isNotificationPermissionRequested = false
        } else if (shouldShowRequestPermissionRationale(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            ShowRequestNotificationPermissionRationaleDialogFragment.show(this)
        } else {
            ShowRequestNotificationPermissionInSettingsRationaleDialogFragment.show(this)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onShowRequestNotificationPermissionInSettingsRationaleResult(
        shouldRequest: Boolean
    ) {
        if (shouldRequest) {
            requestNotificationPermissionInSettings()
        } else {
            viewModel.isNotificationPermissionRequested = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermissionInSettings() {
        requestNotificationPermissionInSettingsLauncher.launch(Unit)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun onRequestNotificationPermissionInSettingsResult(isGranted: Boolean) {
        if (isGranted) {
            viewModel.isNotificationPermissionRequested = false
        }
    }

    override fun showTransfer() {
        binding.transferRecyclerView.isVisible = true
    }

    override fun hideTransfer() {
        binding.transferRecyclerView.isVisible = false
    }

    override fun cancelCloudFileDragDrop() {
        viewModel.clearCloudPasteState()
        binding.dragPlaceholder.fadeToVisibilityUnsafe(false)
    }

    override fun dropToCloud(cloudDirectoryPath: CloudPathData) {
        Log.d("chengluo", "dropToCloud cloudDirectoryPath: $cloudDirectoryPath")
        pasteFiles(cloudDirectoryPath = cloudDirectoryPath)
        binding.dragPlaceholder.fadeToVisibilityUnsafe(false)
    }

    override fun getCloudFileDragShadow(file: CloudFileItem): View {
        binding.iconImage.apply {
            isVisible = true
            val iconRes = file.mimeType.iconRes
            setImageResource(iconRes)
        }
        binding.thumbnailImage.apply {
            dispose()
            setImageDrawable(null)
            val supportsThumbnail = !file.thumbUrl.isNullOrEmpty()
            isVisible = supportsThumbnail
            if (supportsThumbnail) {
                load(file.thumbUrl)
            }
        }
        binding.appIconBadgeImage.apply {
            dispose()
            setImageDrawable(null)
        }
        return binding.dragPlaceholder.apply { isVisible = true }
    }

    override fun clearSelectedCloudFileFiles() {
        viewModel.clearSelectedCloudFiles()
    }

    override fun selectCloudFile(file: CloudFileItem, selected: Boolean) {
        viewModel.selectCloudFile(file, selected)
    }

    override fun selectCloudFiles(files: CloudFileItemSet, selected: Boolean) {
        viewModel.selectCloudFiles(files, selected)
    }

    override fun openCloudFile(file: CloudFileItem) {
        val mimeType = file.mimeType
        if (file.isDirectory) {
            val cloudPath = CloudPathData(file.id, file.name)
            viewModel.navigateCloudTo(cloudPath)
            return
        }
        val url = file.url ?: return
        if (mimeType.isImage) {
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(url.toUri(), mimeType.intentType)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            startActivitySafe(intent)
            return
        }
        if (mimeType.isMedia) {
            val intent = Intent(requireContext(), VideoPlayerActivity::class.java)
                .setDataAndType(url.toUri(), mimeType.intentType)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            VideoPlayerActivity.putExtras(intent, file.name)
            startActivitySafe(intent)
            return
        }
        ConfirmDownloadCloudFileDialogFragment.show(file, this)
    }

    override fun cutCloudFile(file: CloudFileItem) {
        cutCloudFiles(cloudFileItemSetOf(file))
    }

    override fun copyCloudFile(file: CloudFileItem) {
        copyCloudFiles(cloudFileItemSetOf(file))
    }

    override fun confirmDeleteCloudFile(file: CloudFileItem) {
        confirmDeleteCloudFiles(cloudFileItemSetOf(file))
    }

    override fun showRenameCloudFileDialog(file: CloudFileItem) {
        RenameCloudFileDialogFragment.show(file, this)
    }

    override fun showCloudFilePropertiesDialog(file: CloudFileItem) {
        CloudFilePropertiesDialogFragment.show(file, this)
    }

    override fun renameCloudFile(file: CloudFileItem, newName: String) {
        viewModel.renameCloudFile(file, newName)
    }

    override fun deleteCloudFiles(files: CloudFileItemSet) {
        viewModel.deleteCloudFiles(files)
        viewModel.selectCloudFiles(files, false)
    }

    override fun createCloudDirectory(name: String) {
        viewModel.createCloudDirectory(name)
    }

    private fun cutCloudFiles(files: CloudFileItemSet) {
        Log.d("chengluo", "cutCloudFiles: $files")
        viewModel.addToCloudPasteState(false, files)
        viewModel.selectCloudFiles(files, false)
    }

    private fun copyCloudFiles(files: CloudFileItemSet) {
        Log.d("chengluo", "copyCloudFiles: $files")
        viewModel.addToCloudPasteState(true, files)
        viewModel.selectCloudFiles(files, false)
    }

    private fun confirmDeleteCloudFiles(files: CloudFileItemSet) {
        ConfirmDeleteCloudFilesDialogFragment.show(files, this)
    }

    override fun createCloudFile(name: String) {
        viewModel.createCloudFile(name, requireContext())
    }

    override fun downloadToLocal(file: CloudFileItem) {
        viewModel.downloadToLocal(file, requireContext())
    }

    companion object {
        private const val ACTION_VIEW_DOWNLOADS =
            "com.android.imagic.files.intent.action.VIEW_DOWNLOADS"

        private const val IMAGE_VIEWER_ACTIVITY_PATH_LIST_SIZE_MAX = 1000
    }

    private class RequestAllFilesAccessContract : ActivityResultContract<Unit, Boolean>() {
        @RequiresApi(Build.VERSION_CODES.R)
        override fun createIntent(context: Context, input: Unit): Intent =
            Environment::class.createManageAppAllFilesAccessPermissionIntent(context.packageName)

        @RequiresApi(Build.VERSION_CODES.R)
        override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
            Environment.isExternalStorageManager()
    }

    private class RequestPermissionInSettingsContract(private val permissionName: String) :
        ActivityResultContract<Unit, Boolean>() {
        override fun createIntent(context: Context, input: Unit): Intent =
            Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
            application.checkSelfPermissionCompat(permissionName) ==
                    PackageManager.PERMISSION_GRANTED
    }

    @Parcelize
    class Args(val intent: Intent) : ParcelableArgs

    private class Binding private constructor(
        val root: View,
        val drawerLayout: DrawerLayout? = null,
        val persistentDrawerLayout: PersistentDrawerLayout? = null,
        val persistentBarLayout: PersistentBarLayout,
        val appBarLayout: CoordinatorAppBarLayout,
        val toolbar: CrossfadeSubtitleToolbar,
        val overlayToolbar: Toolbar,
        val breadcrumbLayout: BreadcrumbLayout,
        val checkAllLayout: CheckableForegroundLinearLayout,
        val contentLayout: ViewGroup,
        val progress: ProgressBar,
        val errorText: TextView,
        val emptyView: View,
        val swipeRefreshLayout: SwipeRefreshLayout,
        val recyclerView: RecyclerView,
        val cloudRecyclerView: RecyclerView,
        val bottomBarLayout: ViewGroup,
        val bottomProgressbar: ProgressBar,
        val bottomToolbar: Toolbar,
        val bottomCreateFileNameEdit: EditText,
        val speedDialView: SpeedDialView,
        val dragPlaceholder: ConstraintLayout,
        val iconImage: ImageView,
        val thumbnailImage: ImageView,
        val appIconBadgeImage: ImageView,
        val transferRecyclerLayout: FileTransferFloatingLayout,
        val transferRecyclerView: RecyclerView,
        val transferTitle: TextView,
        val transferDropDown: ForegroundImageButton,
        val floatingButton: FloatingButtonView
    ) {
        companion object {
            fun inflate(
                inflater: LayoutInflater,
                root: ViewGroup?,
                attachToRoot: Boolean
            ): Binding {
                val binding = FileListFragmentBinding.inflate(inflater, root, attachToRoot)
                val bindingRoot = binding.root
                val includeBinding = FileListFragmentIncludeBinding.bind(bindingRoot)
                val appBarBinding = FileListFragmentAppBarIncludeBinding.bind(bindingRoot)
                val contentBinding = FileListFragmentContentIncludeBinding.bind(bindingRoot)
                val bottomBarBinding = FileListFragmentBottomBarIncludeBinding.bind(bindingRoot)
                val speedDialBinding = FileListFragmentSpeedDialIncludeBinding.bind(bindingRoot)
                val dragBinding = FileDragPlaceholderBinding.bind(bindingRoot)
                val transferBinding = FileListFragmentTransferIncludeBinding.bind(bindingRoot)
                return Binding(
                    bindingRoot, includeBinding.drawerLayout, includeBinding.persistentDrawerLayout,
                    includeBinding.persistentBarLayout, appBarBinding.appBarLayout,
                    appBarBinding.toolbar, appBarBinding.overlayToolbar,
                    appBarBinding.breadcrumbLayout, appBarBinding.checkAllLayout,
                    contentBinding.contentLayout, contentBinding.progress,
                    contentBinding.errorText, contentBinding.emptyView,
                    contentBinding.swipeRefreshLayout, contentBinding.recyclerView,
                    contentBinding.cloudRecyclerView, bottomBarBinding.bottomBarLayout,
                    bottomBarBinding.bottomProgressbar, bottomBarBinding.bottomToolbar,
                    bottomBarBinding.bottomCreateFileNameEdit, speedDialBinding.speedDialView,
                    dragBinding.dragPlaceholder, dragBinding.iconImage, dragBinding.thumbnailImage,
                    dragBinding.appIconBadgeImage, transferBinding.transferRecyclerLayout,
                    transferBinding.transferRecyclerView, transferBinding.transferTitleText,
                    transferBinding.dropDownButton, transferBinding.floatingButton
                )
            }
        }
    }

    private class MenuBinding private constructor(
        val menu: Menu,
        val searchItem: MenuItem,
        val sortItem: MenuItem,
        val viewTypeItem: MenuItem,
        val sortByNameItem: MenuItem,
        val sortByTypeItem: MenuItem,
        val sortBySizeItem: MenuItem,
        val sortByLastModifiedItem: MenuItem,
        val sortOrderAscendingItem: MenuItem,
        val sortDirectoriesFirstItem: MenuItem,
        val viewSortPathSpecificItem: MenuItem,
        val showHiddenFilesItem: MenuItem,
        val exitApp: MenuItem
    ) {
        companion object {
            fun inflate(menu: Menu, inflater: MenuInflater): MenuBinding {
                inflater.inflate(R.menu.file_list, menu)
                return MenuBinding(
                    menu,
                    menu.findItem(R.id.action_search),
                    menu.findItem(R.id.action_sort),
                    menu.findItem(R.id.action_view_type),
                    menu.findItem(R.id.action_sort_by_name),
                    menu.findItem(R.id.action_sort_by_type),
                    menu.findItem(R.id.action_sort_by_size),
                    menu.findItem(R.id.action_sort_by_last_modified),
                    menu.findItem(R.id.action_sort_order_ascending),
                    menu.findItem(R.id.action_sort_directories_first),
                    menu.findItem(R.id.action_view_sort_path_specific),
                    menu.findItem(R.id.action_show_hidden_files),
                    menu.findItem(R.id.action_exit_app)
                )
            }
        }
    }
}
