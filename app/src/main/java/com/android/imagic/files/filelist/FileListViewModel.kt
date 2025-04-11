package com.android.imagic.files.filelist

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.android.imagic.files.file.FileItem
import com.android.imagic.files.file.FileTypeCategory
import com.android.imagic.files.file.QuickAccessWrap
import com.android.imagic.files.filelist.FileSortOptions.By
import com.android.imagic.files.filelist.FileSortOptions.Order
import com.android.imagic.files.navigation.QuickAccess
import com.android.imagic.files.navigation.QuickAccessCategory
import com.android.imagic.files.provider.archive.archiveRefresh
import com.android.imagic.files.provider.archive.isArchivePath
import com.android.imagic.files.util.CloseableLiveData
import com.android.imagic.files.util.Stateful
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path
import java.io.Closeable

// TODO: Use SavedStateHandle to save state.
class FileListViewModel : ViewModel() {
    private val trailLiveData = TrailLiveData()
    val hasTrail: Boolean
        get() = trailLiveData.value != null
    val pendingState: Parcelable?
        get() = trailLiveData.valueCompat.pendingState

    fun navigateTo(lastState: Parcelable, path: Path) = trailLiveData.navigateTo(lastState, path)

    fun resetTo(path: Path) = trailLiveData.resetTo(path)

    fun navigateUp(): Boolean = if (canNavigateUpBreadcrumb) trailLiveData.navigateUp() else false

    val currentPathLiveData = trailLiveData.map {
        it.currentPath
    }
    val currentPath: Path
        get() = currentPathLiveData.valueCompat

    private val _searchStateLiveData = MutableLiveData(SearchState(false, ""))
    val searchStateLiveData: LiveData<SearchState> = _searchStateLiveData
    val searchState: SearchState
        get() = _searchStateLiveData.valueCompat

    fun search(query: String) {
        val searchState = _searchStateLiveData.valueCompat
        if (searchState.isSearching && searchState.query == query) {
            return
        }
        _searchStateLiveData.value = SearchState(true, query)
    }

    fun stopSearching() {
        val searchState = _searchStateLiveData.valueCompat
        if (!searchState.isSearching) {
            return
        }
        _searchStateLiveData.value = SearchState(false, "")
    }

    private val _quickAccessWrapLiveData = MutableLiveData<QuickAccessWrap?>()
    val quickAccessWrapLiveData: LiveData<QuickAccessWrap?> = _quickAccessWrapLiveData

    fun setQuickAccessWrap(quickAccessWrap: QuickAccessWrap?) {
        if (quickAccessWrapLiveData != quickAccessWrap) {
            _quickAccessWrapLiveData.value = quickAccessWrap
        }
    }

    private val _quickAccessCategoryLiveData = MutableLiveData<QuickAccessCategory?>()
    val quickAccessCategoryLiveData: LiveData<QuickAccessCategory?> = _quickAccessCategoryLiveData
    val quickAccessCategory: QuickAccessCategory?
        get() = quickAccessCategoryLiveData.valueCompat

    val isInQuickAccess: Boolean
        get() = quickAccessCategory != null

    fun setQuickAccessCategory(category: QuickAccessCategory?) {
        if (quickAccessCategory != category) {
            _quickAccessCategoryLiveData.value = category
        }
    }

    fun quickAccessTo(
        context: Context,
        quickAccess: QuickAccess
    ) {
        if (quickAccessCategory == quickAccess.category) return

        val fileTypeCategory = when (quickAccess.category) {
            QuickAccessCategory.IMAGE -> FileTypeCategory.IMAGE
            QuickAccessCategory.VIDEO -> FileTypeCategory.VIDEO
            QuickAccessCategory.AUDIO -> FileTypeCategory.AUDIO
            QuickAccessCategory.DOCUMENT -> FileTypeCategory.DOCUMENT
            QuickAccessCategory.APK -> FileTypeCategory.APK
            QuickAccessCategory.DOWNLOAD -> return
        }

        val title = quickAccess.getTitle(context)
        val quickAccessWrap = QuickAccessWrap(context, title, fileTypeCategory)

        setQuickAccessCategory(quickAccess.category)
        setQuickAccessWrap(quickAccessWrap)
    }

    fun clearQuickAccess() {
        setQuickAccessCategory(null)
        setQuickAccessWrap(null)
    }

    private val _fileListLiveData = FileListSwitchMapLiveData(
        currentPathLiveData,
        _searchStateLiveData,
        _quickAccessWrapLiveData
    )
    val fileListLiveData: LiveData<Stateful<List<FileItem>>>
        get() = _fileListLiveData
    val fileListStateful: Stateful<List<FileItem>>
        get() = _fileListLiveData.valueCompat

    fun reload() {
        val path = currentPath
        if (path.isArchivePath) {
            path.archiveRefresh()
        }
        _fileListLiveData.reload()
    }

    val searchViewExpandedLiveData = MutableLiveData(false)
    var isSearchViewExpanded: Boolean
        get() = searchViewExpandedLiveData.valueCompat
        set(value) {
            if (searchViewExpandedLiveData.valueCompat == value) {
                return
            }
            searchViewExpandedLiveData.value = value
        }

    private val _searchViewQueryLiveData = MutableLiveData("")
    var searchViewQuery: String
        get() = _searchViewQueryLiveData.valueCompat
        set(value) {
            if (_searchViewQueryLiveData.valueCompat == value) {
                return
            }
            _searchViewQueryLiveData.value = value
        }

    val breadcrumbLiveData: LiveData<BreadcrumbData> = BreadcrumbLiveData(
        trailLiveData,
        quickAccessWrapLiveData
    )
    val canNavigateUpBreadcrumb: Boolean
        get() = breadcrumbLiveData.valueCompat.selectedIndex > 0

    private val _viewTypeLiveData = FileViewTypeLiveData(currentPathLiveData)
    val viewTypeLiveData: LiveData<FileViewType> = _viewTypeLiveData
    var viewType: FileViewType
        get() = _viewTypeLiveData.valueCompat
        set(value) {
            _viewTypeLiveData.putValue(value)
        }

    private val _sortOptionsLiveData = FileSortOptionsLiveData(currentPathLiveData)
    val sortOptionsLiveData: LiveData<FileSortOptions> = _sortOptionsLiveData
    val sortOptions: FileSortOptions
        get() = _sortOptionsLiveData.valueCompat

    fun setSortBy(by: By) = _sortOptionsLiveData.putBy(by)

    fun setSortOrder(order: Order) = _sortOptionsLiveData.putOrder(order)

    fun setSortDirectoriesFirst(isDirectoriesFirst: Boolean) =
        _sortOptionsLiveData.putIsDirectoriesFirst(isDirectoriesFirst)

    private val _viewSortPathSpecificLiveData =
        FileViewSortPathSpecificLiveData(currentPathLiveData)
    val viewSortPathSpecificLiveData: LiveData<Boolean>
        get() = _viewSortPathSpecificLiveData
    var isViewSortPathSpecific: Boolean
        get() = _viewSortPathSpecificLiveData.valueCompat
        set(value) {
            _viewSortPathSpecificLiveData.putValue(value)
        }

    private val _pickOptionsLiveData = MutableLiveData<PickOptions?>()
    val pickOptionsLiveData: LiveData<PickOptions?>
        get() = _pickOptionsLiveData
    var pickOptions: PickOptions?
        get() = _pickOptionsLiveData.value
        set(value) {
            _pickOptionsLiveData.value = value
        }

    var isCreateFileNameEditInitialized: Boolean = false

    private val _selectedFilesLiveData = MutableLiveData(fileItemSetOf())
    val selectedFilesLiveData: LiveData<FileItemSet>
        get() = _selectedFilesLiveData
    val selectedFiles: FileItemSet
        get() = _selectedFilesLiveData.valueCompat

    var adapterFiles: List<FileItem> = emptyList()
    private val _isAllSelectedLiveData = MutableLiveData(false)
    val isAllSelectedLiveData: LiveData<Boolean> = _isAllSelectedLiveData

    fun selectFile(file: FileItem, selected: Boolean) {
        selectFiles(fileItemSetOf(file), selected)
    }

    fun selectFiles(files: FileItemSet, selected: Boolean) {
        val selectedFiles = _selectedFilesLiveData.valueCompat
        if (selectedFiles === files) {
            if (!selected && selectedFiles.isNotEmpty()) {
                selectedFiles.clear()
                _selectedFilesLiveData.value = selectedFiles
            }
            return
        }
        var changed = false
        for (file in files) {
            changed = changed or if (selected) {
                selectedFiles.add(file)
            } else {
                selectedFiles.remove(file)
            }
        }
        if (changed) {
            _selectedFilesLiveData.value = selectedFiles
            updateAllSelectedState()
        }
    }

    private fun updateAllSelectedState() {
        val allFiles = adapterFiles.filter { it.isSelectable(pickOptions) }
        _isAllSelectedLiveData.value = allFiles.isNotEmpty() && selectedFiles.containsAll(allFiles)
    }

    fun replaceSelectedFiles(files: FileItemSet) {
        val selectedFiles = _selectedFilesLiveData.valueCompat
        if (selectedFiles == files) {
            return
        }
        selectedFiles.clear()
        selectedFiles.addAll(files)
        _selectedFilesLiveData.value = selectedFiles
    }

    fun clearSelectedFiles() {
        val selectedFiles = _selectedFilesLiveData.valueCompat
        if (selectedFiles.isEmpty()) {
            return
        }
        selectedFiles.clear()
        _selectedFilesLiveData.value = selectedFiles
        updateAllSelectedState()
    }

    val pasteStateLiveData: LiveData<PasteState> = _pasteStateLiveData
    val pasteState: PasteState
        get() = _pasteStateLiveData.valueCompat

    fun addToPasteState(copy: Boolean, files: FileItemSet) {
        val pasteState = _pasteStateLiveData.valueCompat
        var changed = false
        if (pasteState.copy != copy) {
            changed = pasteState.files.isNotEmpty()
            pasteState.files.clear()
            pasteState.copy = copy
        }
        changed = changed or pasteState.files.addAll(files)
        if (changed) {
            _pasteStateLiveData.value = pasteState
        }
    }

    fun clearPasteState() {
        val pasteState = _pasteStateLiveData.valueCompat
        if (pasteState.files.isEmpty()) {
            return
        }
        pasteState.files.clear()
        _pasteStateLiveData.value = pasteState
    }

    private val _isRequestingStorageAccessLiveData = MutableLiveData(false)
    var isStorageAccessRequested: Boolean
        get() = _isRequestingStorageAccessLiveData.valueCompat
        set(value) {
            _isRequestingStorageAccessLiveData.value = value
        }

    private val _isRequestingNotificationPermissionLiveData = MutableLiveData(false)
    var isNotificationPermissionRequested: Boolean
        get() = _isRequestingNotificationPermissionLiveData.valueCompat
        set(value) {
            _isRequestingNotificationPermissionLiveData.value = value
        }

    override fun onCleared() {
        _fileListLiveData.close()
    }

    companion object {
        private val _pasteStateLiveData = MutableLiveData(PasteState())
    }

    private class FileListSwitchMapLiveData(
        private val pathLiveData: LiveData<Path>,
        private val searchStateLiveData: LiveData<SearchState>,
        private val quickAccessWrapLiveData: LiveData<QuickAccessWrap?>
    ) : MediatorLiveData<Stateful<List<FileItem>>>(), Closeable {
        private var liveData: CloseableLiveData<Stateful<List<FileItem>>>? = null

        init {
            addSource(pathLiveData) { updateSource() }
            addSource(searchStateLiveData) { updateSource() }
            addSource(quickAccessWrapLiveData) { updateSource() }
        }

        private fun updateSource() {
            liveData?.let {
                removeSource(it)
                it.close()
            }
            val path = pathLiveData.valueCompat
            val quickAccessWrapData = quickAccessWrapLiveData.valueCompat
            val searchState = searchStateLiveData.valueCompat
            val newLiveData = when {
                quickAccessWrapData != null -> {
                    CategoryFileListLiveData(
                        quickAccessWrapData.context,
                        quickAccessWrapData.fileTypeCategory
                    )
                }

                searchState.isSearching -> {
                    SearchFileListLiveData(path, searchState.query)
                }

                else -> {
                    FileListLiveData(path)
                }
            }
            this.liveData = newLiveData
            addSource(newLiveData) { value = it }
        }

        fun reload() {
            when (val liveData = liveData) {
                is FileListLiveData -> liveData.loadValue()
                is SearchFileListLiveData -> liveData.loadValue()
                is CategoryFileListLiveData -> liveData.loadValue()
            }
        }

        override fun close() {
            liveData?.let {
                removeSource(it)
                it.close()
                this.liveData = null
            }
        }
    }
}
