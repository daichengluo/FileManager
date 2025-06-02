package com.android.imagic.files.filelist

import android.content.Context
import android.os.Environment
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.android.imagic.files.R
import com.android.imagic.files.cloud.gdrive.GoogleDriveClient
import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.cloud.sdrive.client.Client
import com.android.imagic.files.cloud.sdrive.client.TransferStatus
import com.android.imagic.files.file.FileItem
import com.android.imagic.files.file.asFileSize
import com.android.imagic.files.file.cloud.CloudFileItem
import com.android.imagic.files.filejob.FileJobService
import com.android.imagic.files.navigation.QuickAccessCategory
import com.android.imagic.files.navigation.getExternalStorageDirectory
import com.android.imagic.files.provider.archive.archiveRefresh
import com.android.imagic.files.provider.archive.isArchivePath
import com.android.imagic.files.provider.common.createDirectory
import com.android.imagic.files.provider.common.createFile
import com.android.imagic.files.provider.common.isDirectory
import com.android.imagic.files.provider.common.newDirectoryStream
import com.android.imagic.files.provider.common.size
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.CloseableLiveData
import com.android.imagic.files.util.Failure
import com.android.imagic.files.util.Loading
import com.android.imagic.files.util.Stateful
import com.android.imagic.files.util.Success
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path
import java8.nio.file.Paths
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.io.IOException

// TODO: Use SavedStateHandle to save state.
class FileListViewModel : ViewModel() {
    private val trailLiveData = TrailLiveData()
    val hasTrail: Boolean
        get() = trailLiveData.value != null
    val pendingState: Parcelable?
        get() = trailLiveData.valueCompat.pendingState

    fun navigateTo(lastState: Parcelable, path: Path) = trailLiveData.navigateTo(lastState, path)

    fun resetTo(path: Path) = trailLiveData.resetTo(path)

    fun navigateUp(): Boolean = if (canNavigateUpBreadcrumb) {
        if (isCloudFileMode) {
            navigateCloudUp()
            true
        } else {
            trailLiveData.navigateUp()
        }
    } else false

    private val _currentFileModeStateLiveData = MutableLiveData<FileModeState>(FileModeState.Local)
    val currentFileModeStateLiveData: LiveData<FileModeState> = _currentFileModeStateLiveData
    val currentFileMode: FileModeState
        get() = currentFileModeStateLiveData.valueCompat

    val isCloudFileMode: Boolean
        get() = currentFileModeStateLiveData.valueCompat is FileModeState.Cloud

    fun setFileModeState(mode: FileModeState) {
        if (_currentFileModeStateLiveData.valueCompat != mode) {
            _currentFileModeStateLiveData.value = mode
        }
    }

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

    val currentQuickAccessCategory: QuickAccessCategory?
        get() {
            val fileModeState = currentFileModeStateLiveData.valueCompat
            return if (fileModeState is FileModeState.QuickAccess) {
                fileModeState.quickAccessWrap.quickAccess.category
            } else null
        }

    private val _fileListLiveData = FileListSwitchMapLiveData(
        currentFileModeStateLiveData,
        currentPathLiveData,
        _searchStateLiveData
    )
    val fileListLiveData: LiveData<Stateful<List<FileItem>>>
        get() = _fileListLiveData
    val fileListStateful: Stateful<List<FileItem>>
        get() = _fileListLiveData.valueCompat

    private val _cloudAuthority = MutableLiveData<Authority?>(null)
    val cloudAuthority: LiveData<Authority?> = _cloudAuthority
    val authority: Authority?
        get() = cloudAuthority.valueCompat

    fun setCloudAuthority(authority: Authority?) {
        if (_cloudAuthority != authority) {
            _cloudAuthority.value = authority
            _cloudPathListLiveData.value = null
        }
    }

    fun logout(authority: Authority) {
        if (_cloudAuthority.valueCompat == authority) {
            setFileModeState(FileModeState.Local)
            setCloudAuthority(null)
        }
    }

    private val _cloudPathListLiveData = MutableLiveData<List<CloudPathData>?>(null)
    val cloudPathListLiveData: LiveData<List<CloudPathData>?> = _cloudPathListLiveData
    val currentCloudPathLiveData = cloudPathListLiveData.map {
        it?.lastOrNull()
    }

    private val _cloudFileListLiveData = MutableLiveData<Stateful<List<CloudFileItem>>>()
    val cloudFileListLiveData: LiveData<Stateful<List<CloudFileItem>>> = _cloudFileListLiveData

    val cloudFileListStateful: Stateful<List<CloudFileItem>>
        get() = cloudFileListLiveData.valueCompat

    val currentCloudPath: CloudPathData?
        get() = currentCloudPathLiveData.valueCompat

    fun refreshCloud() {
        currentCloudPath?.let { navigateCloudTo(it) }
    }

    fun navigateCloudTo(pathData: CloudPathData) {
        if (authority == null) return
        val cloudPathList = _cloudPathListLiveData.valueCompat ?: emptyList()
        val index = cloudPathList.indexOfFirst { it == pathData }

        val newList = if (index == -1) {
            cloudPathList + pathData
        } else {
            cloudPathList.subList(0, index + 1)
        }
        _cloudPathListLiveData.value = newList
    }

    fun loadCloudFileList(path: CloudPathData) = viewModelScope.launch {
        val currentAuthority = authority ?: return@launch
        _cloudFileListLiveData.value = Loading(null)
        val response = Client.getFileList(path.directoryId, currentAuthority)
        _cloudFileListLiveData.value = when (response) {
            is ApiOperation.Success -> Success(response.data)
            is ApiOperation.Failure -> Failure(null, response.exception)
        }
    }

    fun navigateCloudUp() {
        if (authority == null) return
        val cloudPathList = _cloudPathListLiveData.valueCompat ?: return
        if (cloudPathList.isEmpty()) return

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

    fun createCloudFile(name: String, context: Context) {
        try {
            val newFile = File(context.cacheDir, name)
            newFile.deleteOnExit()
            uploadFile(newFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    fun downloadToLocal(file: CloudFileItem, context: Context) {
        val downloadPath = Paths.get(getExternalStorageDirectory(Environment.DIRECTORY_DOWNLOADS))
        downloadFile(file, downloadPath, false, context)
    }

    suspend fun uploadFolderRecursively(
        path: Path,
        parentDirectoryId: Int,
        isMove: Boolean,
        context: Context
    ) {
        if (authority == null) return

        if (path.isDirectory()) {
            val folderName = path.fileName.toString()

            val response = Client.createDirectory(folderName, parentDirectoryId, authority!!)
            val newDirectoryId = if (response is ApiOperation.Success) {
                refreshCloud()
                response.data.id
            } else null
            if (newDirectoryId == null) {
                Log.e("chengluo", "Failed to create remote folder: $folderName")
                return
            }

            path.newDirectoryStream().use { directoryStream ->
                for (child in directoryStream) {
                    uploadFolderRecursively(child, newDirectoryId, isMove, context)
                }
            }

            if (isMove) {
                FileJobService.delete(listOf(path), context)
            }
        } else {
            uploadFile(path, parentDirectoryId, isMove, context)
        }
    }

    fun uploadFile(localFile: Path, directoryId: Int, isMove: Boolean, context: Context) =
        viewModelScope.launch {
            if (authority == null) return@launch
            val name = localFile.fileName.toString()
            val sizeInBytes = localFile.size()
            val sizeInKB = sizeInBytes / 1024
            val isBigFile = sizeInKB > 1024 * 5
            val id = localFile.toString().hashCode()

            if (isBigFile) {
                Client.uploadChunks(localFile, authority!!).collect { status ->
                    Log.d("chengluo", "uploadFile status: $status")
                    val transfer = when (status) {
                        is TransferStatus.Failed -> FileTransfer(
                            id = id,
                            title = name,
                            text = "Failed: ${status.exception.message}",
                            max = 100,
                            progress = 0,
                            indeterminate = false,
                            status = FileTransfer.Status.FAILED
                        )

                        is TransferStatus.Finished -> {
                            val url = status.data ?: return@collect
                            Client.uploadFileInfo(name, sizeInKB, url, directoryId, authority!!)
                                .onSuccess {
                                    refreshCloud()
                                    if (isMove) {
                                        FileJobService.delete(listOf(localFile), context)
                                    }
                                }
                                .onFailure { it.printStackTrace() }
                            FileTransfer(
                                id = id,
                                title = name,
                                text = "Completed",
                                max = 100,
                                progress = 100,
                                indeterminate = false,
                                status = FileTransfer.Status.FINISHED
                            )
                        }

                        is TransferStatus.Transferring -> {
                            val sizeString = status.total.asFileSize().formatHumanReadable(context)
                            val transferredSizeString =
                                status.transferred.asFileSize().formatHumanReadable(context)
                            val text = context.getString(
                                R.string.file_job_transfer_size_notification_text_one_format,
                                transferredSizeString,
                                sizeString
                            )
                            FileTransfer(
                                id = id,
                                title = name,
                                text = text,
                                max = status.total.toInt(),
                                progress = status.transferred.toInt(),
                                indeterminate = false,
                                status = FileTransfer.Status.TRANSFERRING
                            )
                        }
                    }
                    _fileTransferState.emit(transfer)
                }
            } else {
                val response = Client.uploadFile(localFile, authority!!)
                if (response is ApiOperation.Success) {
                    val url = response.data.url ?: ""
                    Log.d("chengluo", "uploadFile file size: $sizeInKB")
                    Client.uploadFileInfo(name, sizeInKB, url, directoryId, authority!!)
                        .onSuccess {
                            refreshCloud()
                            if (isMove) {
                                FileJobService.delete(listOf(localFile), context)
                            }
                        }
                        .onFailure { it.printStackTrace() }
                }
            }
        }

    fun uploadFile(file: File) = viewModelScope.launch {
        if (authority == null) return@launch
        val directoryId = currentCloudPath?.directoryId ?: return@launch
        val response = Client.uploadFile(file, authority!!)
        if (response is ApiOperation.Success) {
            val url = response.data.url ?: ""
            val sizeInKB = file.length() / 1024
            Log.d("chengluo", "uploadFile file size: $sizeInKB")
            Client.uploadFileInfo(file.name, sizeInKB, url, directoryId, authority!!)
                .onSuccess {
                    refreshCloud()
                }
                .onFailure { it.printStackTrace() }
        }
    }

    private val _fileTransferState = MutableSharedFlow<FileTransfer>()
    val fileTransferState: SharedFlow<FileTransfer> = _fileTransferState

    suspend fun downloadCloudRecursively(
        cloudFile: CloudFileItem,
        path: Path,
        isMove: Boolean,
        context: Context
    ) {
        val currentAuthority = authority ?: return
        if (cloudFile.isDirectory) {
            val localDir = path.resolve(cloudFile.name)
            localDir.createDirectory()

            val response = Client.getFileList(cloudFile.id, currentAuthority)

            when (response) {
                is ApiOperation.Success -> {
                    val children = response.data
                    for (child in children) {
                        downloadCloudRecursively(child, localDir, isMove, context)
                    }
                    if (isMove) {
                        deleteCloudFiles(cloudFileItemSetOf(cloudFile))
                    }
                }

                is ApiOperation.Failure -> {
                    Log.e(
                        "chengluo",
                        "Failed to list children for folder: ${cloudFile.name}",
                        response.exception
                    )
                }
            }
        } else {
            downloadFile(cloudFile, path, isMove, context)
        }
    }

    fun downloadFile(
        cloudFile: CloudFileItem,
        directoryPath: Path,
        isMove: Boolean,
        context: Context
    ) =
        viewModelScope.launch {
            try {
                val localFile = directoryPath.resolve(cloudFile.name)
                localFile.createFile()
                if (authority == null || cloudFile.url == null) return@launch
                val id = cloudFile.hashCode()
                Client.downloadFile(cloudFile.url, localFile, authority!!).collect { status ->
                    val transfer = when (status) {
                        is TransferStatus.Transferring -> {
                            val sizeString = status.total.asFileSize().formatHumanReadable(context)
                            val transferredSizeString =
                                status.transferred.asFileSize().formatHumanReadable(context)
                            val text = context.getString(
                                R.string.file_job_transfer_size_notification_text_one_format,
                                transferredSizeString,
                                sizeString
                            )
                            FileTransfer(
                                id = id,
                                title = cloudFile.name,
                                text = text,
                                max = status.total.toInt(),
                                progress = status.transferred.toInt(),
                                indeterminate = false,
                                status = FileTransfer.Status.TRANSFERRING
                            )
                        }

                        is TransferStatus.Finished -> {
                            if (isMove) {
                                deleteCloudFiles(cloudFileItemSetOf(cloudFile))
                            }
                            FileTransfer(
                                id = id,
                                title = cloudFile.name,
                                text = "Completed",
                                max = 100,
                                progress = 100,
                                indeterminate = false,
                                status = FileTransfer.Status.FINISHED
                            )
                        }

                        is TransferStatus.Failed -> FileTransfer(
                            id = id,
                            title = cloudFile.name,
                            text = "Failed: ${status.exception.message}",
                            max = 100,
                            progress = 0,
                            indeterminate = false,
                            status = FileTransfer.Status.FAILED
                        )
                    }
                    _fileTransferState.emit(transfer)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                _fileTransferState.emit(
                    FileTransfer(
                        id = cloudFile.hashCode(),
                        title = cloudFile.name,
                        text = "Failed: ${e.message}",
                        max = 100,
                        progress = 0,
                        indeterminate = false,
                        status = FileTransfer.Status.FAILED
                    )
                )
            }
        }

    fun pasteToLocal(targetLocalDir: Path, context: Context) {
        val cloudFiles = cloudPasteState.files
        if (cloudPasteState.copy) {
            cloudFiles.forEach { cloudFile ->
                viewModelScope.launch {
                    downloadCloudRecursively(cloudFile, targetLocalDir, false, context)
                }
            }
        } else {
            cloudFiles.forEach { cloudFile ->
                viewModelScope.launch {
                    downloadCloudRecursively(cloudFile, targetLocalDir, true, context)
                }
            }
        }
        clearCloudPasteState()

        val files = pasteState.files.map { it.path }.sortedBy { it.toUri() }
        if (pasteState.copy) {
            FileJobService.copy(files, targetLocalDir, context)
        } else {
            FileJobService.move(files, targetLocalDir, context)
        }
        clearPasteState()
    }

    fun pasteToCloud(targetCloudPath: CloudPathData, context: Context) {
        val cloudFiles = cloudPasteState.files
        if (cloudPasteState.copy) {
            copyCloudFiles(cloudFiles, targetCloudPath)
        } else {
            moveCloudFiles(cloudFiles, targetCloudPath)
        }
        clearCloudPasteState()

        val files = pasteState.files
        val directoryId = currentCloudPath?.directoryId ?: 0
        if (pasteState.copy) {
            files.forEach {
                viewModelScope.launch {
                    uploadFolderRecursively(it.path, directoryId, false, context)
                }
            }
        } else {
            files.forEach {
                viewModelScope.launch {
                    uploadFolderRecursively(it.path, directoryId, true, context)
                }
            }
        }
        clearPasteState()
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

    fun getGoogleDriveRootDirectoryFiles(accessToken: String) = viewModelScope.launch {
        Log.d("chengluo", "getGoogleDriveRootDirectoryFiles accessToken: $accessToken")
        GoogleDriveClient.getRootDirectoryFiles(accessToken)
    }

    fun createGoogleDriveFolder(accessToken: String, folderName: String) = viewModelScope.launch {
        Log.d(
            "chengluo",
            "createGoogleDriveFolder folderName: $folderName accessToken: $accessToken"
        )
        GoogleDriveClient.createFolder(accessToken, folderName)
        delay(3000)
        getGoogleDriveRootDirectoryFiles(accessToken)
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
        currentFileModeStateLiveData,
        trailLiveData,
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
            if (isCloudFileMode) {
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
        private val fileModeStateLiveData: LiveData<FileModeState>,
        private val pathLiveData: LiveData<Path>,
        private val searchStateLiveData: LiveData<SearchState>
    ) : MediatorLiveData<Stateful<List<FileItem>>>(), Closeable {
        private var liveData: CloseableLiveData<Stateful<List<FileItem>>>? = null

        init {
            addSource(fileModeStateLiveData) { updateSource() }
            addSource(pathLiveData) { updateSource() }
            addSource(searchStateLiveData) { updateSource() }
        }

        private fun updateSource() {
            liveData?.let {
                removeSource(it)
                it.close()
                liveData = null
            }
            val path = pathLiveData.valueCompat
            val fileModeState = fileModeStateLiveData.valueCompat
            val searchState = searchStateLiveData.valueCompat

            val newLiveData = if (searchState.isSearching) {
                SearchFileListLiveData(path, searchState.query)
            } else {
                when (fileModeState) {
                    FileModeState.Local -> FileListLiveData(path)
                    is FileModeState.Cloud -> return
                    is FileModeState.QuickAccess -> {
                        val quickAccessWrap = fileModeState.quickAccessWrap
                        if (quickAccessWrap.category == null) {
                            // Download category use path
                            FileListLiveData(path)
                        } else {
                            CategoryFileListLiveData(
                                quickAccessWrap.context,
                                quickAccessWrap.category!!
                            )
                        }
                    }
                }
            }
            liveData = newLiveData
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
                liveData = null
            }
        }
    }
}
