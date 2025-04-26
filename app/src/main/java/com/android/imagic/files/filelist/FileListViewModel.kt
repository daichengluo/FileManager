package com.android.imagic.files.filelist

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.android.imagic.files.file.FileItem
import com.android.imagic.files.file.FileTypeCategory
import com.android.imagic.files.file.QuickAccessWrap
import com.android.imagic.files.file.cloud.sdrive.CloudFileItem
import com.android.imagic.files.navigation.QuickAccess
import com.android.imagic.files.navigation.QuickAccessCategory
import com.android.imagic.files.provider.archive.archiveRefresh
import com.android.imagic.files.provider.archive.isArchivePath
import com.android.imagic.files.provider.cloud.sdrive.client.Authority
import com.android.imagic.files.provider.cloud.sdrive.client.Client
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.CloseableLiveData
import com.android.imagic.files.util.Failure
import com.android.imagic.files.util.Loading
import com.android.imagic.files.util.Stateful
import com.android.imagic.files.util.Success
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path
import kotlinx.coroutines.launch
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

    val isInQuickAccess: Boolean get() = quickAccessCategory != null

    fun setQuickAccessCategory(category: QuickAccessCategory?) {
        if (quickAccessCategory != category) {
            _quickAccessCategoryLiveData.value = category
        }
    }

    fun quickAccessTo(context: Context, quickAccess: QuickAccess) {
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

    private val _cloudAuthority = MutableLiveData<Authority?>(null)
    val cloudAuthority: LiveData<Authority?> = _cloudAuthority
    val authority: Authority?
        get() = cloudAuthority.valueCompat

    val isInCloudStorage: LiveData<Boolean> = cloudAuthority.map { it != null }

    private val _cloudPathListLiveData = MutableLiveData<List<CloudPathData>?>(null)
    val cloudPathListLiveData: LiveData<List<CloudPathData>?> = _cloudPathListLiveData
    val currentCloudPathLiveData = cloudPathListLiveData.map {
        it?.lastOrNull()
    }

    val cloudFileListLiveData: LiveData<Stateful<List<CloudFileItem>>> =
        currentCloudPathLiveData.switchMap { cloudPath ->
            liveData {
                if (authority != null && cloudPath != null) {
                    emit(Loading(null))
                    val response = Client.getFileList(cloudPath.directoryId, authority!!)
                    if (response is ApiOperation.Success) {
                        emit(Success(response.data))
                    } else if (response is ApiOperation.Failure) {
                        emit(Failure(null, response.exception))
                    }
                }
            }
        }
    val cloudFileListStateful: Stateful<List<CloudFileItem>>
        get() = cloudFileListLiveData.valueCompat

    val currentCloudPath: CloudPathData?
        get() = currentCloudPathLiveData.valueCompat

    fun clearAuthority() {
        if (authority != null) {
            _cloudAuthority.value = null
        }
        if (_cloudPathListLiveData.valueCompat != null) {
            _cloudPathListLiveData.value = null
        }
    }

    fun refreshCloud() {
        currentCloudPath?.let { navigateCloudTo(it) }
    }

    fun onCloudStorageClicked(authority: Authority) = viewModelScope.launch {
        if (_cloudPathListLiveData.valueCompat == null) {
            _cloudPathListLiveData.value = listOf(CloudPathData(0, authority.username))
        }
        _cloudAuthority.value = authority
        val rootCloudPath = CloudPathData(0, authority.username)
        navigateCloudTo(rootCloudPath)
    }

    fun navigateCloudTo(cloudPathData: CloudPathData) = viewModelScope.launch {
        if (authority == null) return@launch
        val cloudPathList = _cloudPathListLiveData.valueCompat ?: emptyList()
        val index = cloudPathList.indexOfFirst { it.directoryId == cloudPathData.directoryId }

        val newList = if (index == -1) {
            cloudPathList.plus(cloudPathData)
        } else {
            cloudPathList.subList(0, index + 1)
        }
        _cloudPathListLiveData.value = newList
    }

    fun navigateCloudUp() = viewModelScope.launch {
        if (authority == null) return@launch
        val cloudPathList = _cloudPathListLiveData.valueCompat ?: return@launch
        if (cloudPathList.isEmpty()) return@launch

        val newList = cloudPathList.dropLast(1)
        _cloudPathListLiveData.value = newList
    }

    fun createCloudDirectory(name: String) = viewModelScope.launch {
        if (authority == null) return@launch
        val currentCloudPath = currentCloudPath ?: return@launch
        Client.createDirectory(name, currentCloudPath.directoryId, authority!!)
            .onSuccess { refreshCloud() }
            .onFailure { it.printStackTrace() }
    }

    fun renameCloudFile(file: CloudFileItem, newName: String) = viewModelScope.launch {
        if (authority == null) return@launch
        Client.renameFile(newName, file.id, authority!!)
            .onSuccess { refreshCloud() }
            .onFailure { it.printStackTrace() }
    }

    fun deleteCloudFiles(files: CloudFileItemSet) = viewModelScope.launch {
        if (authority == null || files.isEmpty()) return@launch
        Client.delete(files.map { it.id }, authority!!)
            .onSuccess { refreshCloud() }
            .onFailure { it.printStackTrace() }
    }

    fun pasteCloudFiles() {
        val targetCloudPath = currentCloudPath ?: return
        val files = cloudPasteState.files
        if (cloudPasteState.copy) {
            copyCloudFiles(files, targetCloudPath)
        } else {
            moveCloudFiles(files, targetCloudPath)
        }
        clearCloudPasteState()
    }

    private fun moveCloudFiles(files: CloudFileItemSet, cloudPath: CloudPathData) {
        viewModelScope.launch {
            if (authority == null || files.isEmpty()) return@launch
            Client.move(files.map { it.id }, cloudPath.directoryId, authority!!)
                .onSuccess { refreshCloud() }
                .onFailure { it.printStackTrace() }
        }
    }

    private fun copyCloudFiles(files: CloudFileItemSet, cloudPath: CloudPathData) {
        viewModelScope.launch {
            if (authority == null || files.isEmpty()) return@launch
            Client.copy(files.map { it.id }, cloudPath.directoryId, authority!!)
                .onSuccess { refreshCloud() }
                .onFailure { it.printStackTrace() }
        }
    }

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
        quickAccessWrapLiveData,
        cloudPathListLiveData
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

    private fun getSelectableVisibleFiles(): List<FileItem> {
        val stateful = _fileListLiveData.valueCompat
        if (stateful !is Success) return emptyList()
        val files = fileListStateful.value.orEmpty()
        val isShowHiddenFiles = Settings.FILE_LIST_SHOW_HIDDEN_FILES.valueCompat
        val visibleFiles = if (isShowHiddenFiles) files else files.filterNot { it.isHidden }
        return visibleFiles.filter { it.isSelectable(pickOptions) }
    }

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
        }
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
    }

    private val _selectedCloudFilesLiveData = MutableLiveData(cloudFileItemSetOf())
    val selectedCloudFilesLiveData: LiveData<CloudFileItemSet>
        get() = _selectedCloudFilesLiveData
    val selectedCloudFiles: CloudFileItemSet
        get() = _selectedCloudFilesLiveData.valueCompat

    fun selectCloudFile(file: CloudFileItem, selected: Boolean) {
        selectCloudFiles(cloudFileItemSetOf(file), selected)
    }

    fun selectCloudFiles(files: CloudFileItemSet, selected: Boolean) {
        val selectedCloudFiles = _selectedCloudFilesLiveData.valueCompat
        if (selectedCloudFiles === files) {
            if (!selected && selectedCloudFiles.isNotEmpty()) {
                selectedCloudFiles.clear()
                _selectedCloudFilesLiveData.value = selectedCloudFiles
            }
            return
        }
        var changed = false
        for (file in files) {
            changed = changed or if (selected) {
                selectedCloudFiles.add(file)
            } else {
                selectedCloudFiles.remove(file)
            }
        }
        if (changed) {
            _selectedCloudFilesLiveData.value = selectedCloudFiles
        }
    }

    fun clearSelectedCloudFiles() {
        val selectedCloudFiles = _selectedCloudFilesLiveData.valueCompat
        if (selectedCloudFiles.isEmpty()) {
            return
        }
        selectedCloudFiles.clear()
        _selectedCloudFilesLiveData.value = selectedCloudFiles
    }

    val isAllSelectedLiveData = MediatorLiveData<Boolean>().apply {
        val compute = {
            val authority = cloudAuthority.value
            if (authority != null) {
                val stateful = cloudFileListLiveData.value
                if (stateful !is Success) {
                    value = false
                }

                val files = stateful?.value.orEmpty()
                value = files.isNotEmpty() && selectedCloudFiles.containsAll(files)
            } else {
                val stateful = _fileListLiveData.value
                if (stateful !is Success) {
                    value = false
                }

                val files = stateful?.value.orEmpty()
                val isShowHidden = Settings.FILE_LIST_SHOW_HIDDEN_FILES.valueCompat
                val visibleFiles = if (isShowHidden) files else files.filterNot { it.isHidden }
                val selectableFiles = visibleFiles.filter { it.isSelectable(pickOptions) }

                value = selectableFiles.isNotEmpty() && selectedFiles.containsAll(selectableFiles)
            }
        }

        addSource(cloudAuthority) { compute() }
        addSource(_selectedFilesLiveData) { compute() }
        addSource(_fileListLiveData) { compute() }
        addSource(_selectedCloudFilesLiveData) { compute() }
        addSource(cloudFileListLiveData) { compute() }
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

    val cloudPasteStateLiveData: LiveData<CloudPasteState> = _cloudPasteStateLiveData
    val cloudPasteState: CloudPasteState
        get() = _cloudPasteStateLiveData.valueCompat

    fun addToCloudPasteState(copy: Boolean, files: CloudFileItemSet) {
        val cloudPasteState = _cloudPasteStateLiveData.valueCompat
        var changed = false
        if (cloudPasteState.copy != copy) {
            changed = cloudPasteState.files.isNotEmpty()
            cloudPasteState.files.clear()
            cloudPasteState.copy = copy
        }
        changed = changed or cloudPasteState.files.addAll(files)
        if (changed) {
            _cloudPasteStateLiveData.value = cloudPasteState
        }
    }

    fun clearCloudPasteState() {
        val cloudPasteState = _cloudPasteStateLiveData.valueCompat
        if (cloudPasteState.files.isEmpty()) {
            return
        }
        cloudPasteState.files.clear()
        _cloudPasteStateLiveData.value = cloudPasteState
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
        private val _cloudPasteStateLiveData = MutableLiveData(CloudPasteState())
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
