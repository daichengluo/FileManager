package com.android.imagic.files.filelist

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.android.imagic.files.R
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.gdrive.api.GoogleDriveClient
import com.android.imagic.files.cloud.gdrive.sdk.GoogleDriveServiceClient
import com.android.imagic.files.cloud.onedrive.OneDriveClient
import com.android.imagic.files.cloud.sdrive.client.StarBoardDriveClient
import com.android.imagic.files.cloud.util.SpaceData
import com.android.imagic.files.cloud.util.TransferJobManager
import com.android.imagic.files.cloud.util.TransferStatus
import com.android.imagic.files.file.FileItem
import com.android.imagic.files.file.asFileSize
import com.android.imagic.files.filejob.FileJobService
import com.android.imagic.files.navigation.QuickAccessCategory
import com.android.imagic.files.provider.archive.archiveRefresh
import com.android.imagic.files.provider.archive.isArchivePath
import com.android.imagic.files.provider.common.createDirectory
import com.android.imagic.files.provider.common.createFile
import com.android.imagic.files.provider.common.deleteIfExists
import com.android.imagic.files.provider.common.exists
import com.android.imagic.files.provider.common.isDirectory
import com.android.imagic.files.provider.common.isEmptyDirectory
import com.android.imagic.files.provider.common.newDirectoryStream
import com.android.imagic.files.provider.common.size
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.storage.GoogleDriveServer
import com.android.imagic.files.storage.OneDriveServer
import com.android.imagic.files.storage.StarBoardDriveServer
import com.android.imagic.files.storage.Storages
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.CloseableLiveData
import com.android.imagic.files.util.Failure
import com.android.imagic.files.util.Loading
import com.android.imagic.files.util.ScreenMode
import com.android.imagic.files.util.Stateful
import com.android.imagic.files.util.Success
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File

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

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _currentFileModeStateLiveData = MutableLiveData<FileModeState>(FileModeState.Local)
    val currentFileModeStateLiveData: LiveData<FileModeState> = _currentFileModeStateLiveData
    val currentFileMode: FileModeState
        get() = currentFileModeStateLiveData.valueCompat

    val isCloudFileMode: Boolean
        get() = currentFileMode is FileModeState.StarBoardDrive ||
                currentFileMode is FileModeState.GoogleDrive ||
                currentFileMode is FileModeState.OneDrive

    val isLocalFileMode: Boolean
        get() = currentFileMode is FileModeState.Local

    fun setFileModeState(mode: FileModeState) {
        if (_currentFileModeStateLiveData.valueCompat != mode) {
            _cloudPathListLiveData.value = emptyList()
        }
        _currentFileModeStateLiveData.value = mode
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

    private val _screenModeLiveData = MutableLiveData(ScreenMode.STANDARD)
    val screenModeLiveData: LiveData<ScreenMode> = _screenModeLiveData
    var screenMode: ScreenMode
        get() = _screenModeLiveData.valueCompat
        set(value) {
            _screenModeLiveData.value = value
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

    fun logout() {
        setFileModeState(FileModeState.Local)
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
        updateCloudPath(pathData)
    }

    private fun updateCloudPath(pathData: CloudPathData) {
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

        val cloudMode = currentFileMode

        if (cloudMode is FileModeState.StarBoardDrive) {
            _cloudFileListLiveData.value = Loading(null)

            val response = StarBoardDriveClient.getFileList(path.directoryId, cloudMode.authority)

            _cloudFileListLiveData.value = when (response) {
                is ApiOperation.Success -> Success(response.data)
                is ApiOperation.Failure -> Failure(null, response.exception)
            }
        }
        if (cloudMode is FileModeState.GoogleDrive) {
            _cloudFileListLiveData.value = Loading(null)

            val response = if (cloudMode.authority.accessToken.isEmpty()) {
                GoogleDriveServiceClient.getFileList(path.directoryId, cloudMode.authority)
            } else {
                GoogleDriveClient.getFileList(path.directoryId, cloudMode.authority)
            }
            _cloudFileListLiveData.value = when (response) {
                is ApiOperation.Success -> Success(response.data)
                is ApiOperation.Failure -> Failure(null, response.exception)
            }
        }
        if (cloudMode is FileModeState.OneDrive) {
            _cloudFileListLiveData.value = Loading(null)

            val response = if (path.isRoot) {
                OneDriveClient.getRootFileList(cloudMode.authority)
            } else {
                OneDriveClient.getFileList(path.directoryId, cloudMode.authority)
            }

            _cloudFileListLiveData.value = when (response) {
                is ApiOperation.Success -> Success(response.data)
                is ApiOperation.Failure -> Failure(null, response.exception)
            }
        }
    }

    private fun navigateCloudUp() {
        val cloudPathList = _cloudPathListLiveData.valueCompat ?: return
        if (cloudPathList.isEmpty()) return

        val newList = cloudPathList.dropLast(1)
        _cloudPathListLiveData.value = newList
    }

    fun createCloudDirectory(name: String) = viewModelScope.launch {
        val currentCloudPath = currentCloudPath ?: return@launch

        val cloudMode = currentFileMode

        if (cloudMode is FileModeState.StarBoardDrive) {
            StarBoardDriveClient.createDirectory(
                name,
                currentCloudPath.directoryId,
                cloudMode.authority
            )
                .onSuccess { refreshCloud() }
                .onFailure { it.printStackTrace() }
        }
        if (cloudMode is FileModeState.GoogleDrive) {
            if (cloudMode.authority.accessToken.isEmpty()) {
                GoogleDriveServiceClient.createDirectory(
                    name,
                    currentCloudPath.directoryId,
                    cloudMode.authority
                )
                refreshCloud()
            } else {
                GoogleDriveClient.createDirectory(
                    name,
                    currentCloudPath.directoryId,
                    cloudMode.authority
                )
                    .onSuccess { refreshCloud() }
                    .onFailure { it.printStackTrace() }
            }
        }
        if (cloudMode is FileModeState.OneDrive) {
            OneDriveClient.createDirectory(name, currentCloudPath.directoryId, cloudMode.authority)
                .onSuccess { refreshCloud() }
                .onFailure { it.printStackTrace() }
        }
    }

    fun createCloudFile(name: String, context: Context) {
        val file = File(context.cacheDir, name)
        file.deleteOnExit()
        uploadFile(file, context)
    }

    fun renameCloudFile(file: CloudFileItem, newName: String) = viewModelScope.launch {
        val cloudMode = currentFileMode
        if (cloudMode is FileModeState.StarBoardDrive) {
            StarBoardDriveClient.renameFile(newName, file.id, cloudMode.authority)
                .onSuccess { refreshCloud() }
                .onFailure { it.printStackTrace() }
        }
        if (cloudMode is FileModeState.GoogleDrive) {
            if (cloudMode.authority.accessToken.isEmpty()) {
                GoogleDriveServiceClient.renameFile(file.id, newName, cloudMode.authority)
                refreshCloud()
            } else {
                GoogleDriveClient.renameFile(file.id, newName, cloudMode.authority)
                    .onSuccess { refreshCloud() }
                    .onFailure { it.printStackTrace() }
            }
        }
        if (cloudMode is FileModeState.OneDrive) {
            OneDriveClient.renameFile(file.id, newName, cloudMode.authority)
                .onSuccess { refreshCloud() }
                .onFailure { it.printStackTrace() }
        }
    }

    fun deleteCloudFiles(files: CloudFileItemSet, cloudMode: FileModeState) =
        viewModelScope.launch {
            val fileList = files.toList()
            Log.d("chengluo", "deleteCloudFiles: $fileList cloudMode: $currentFileMode")
            if (fileList.isEmpty()) return@launch
            if (cloudMode is FileModeState.StarBoardDrive) {
                Log.d("chengluo", "deleteCloudFiles: StarBoardDrive")
                StarBoardDriveClient.delete(fileList.map { it.id }, cloudMode.authority)
                    .onFailure { Log.e("chengluo", "StarBoardDrive deleteCloudFiles: ", it) }
                getDriveSpace(cloudMode)
            }
            if (cloudMode is FileModeState.GoogleDrive) {
                Log.d("chengluo", "deleteCloudFiles: GoogleDrive")
                for (file in fileList) {
                    if (cloudMode.authority.accessToken.isEmpty()) {
                        GoogleDriveServiceClient.deleteFile(file.id, cloudMode.authority)
                    } else {
                        GoogleDriveClient.deleteFileById(file.id, cloudMode.authority)
                    }
                }
            }
            if (cloudMode is FileModeState.OneDrive) {
                Log.d("chengluo", "deleteCloudFiles: OneDrive")
                for (file in fileList) {
                    OneDriveClient.deleteFileById(file.id, cloudMode.authority)
                }
            }
            refreshCloud()
        }

    private fun getDriveSpace(cloudMode: FileModeState) = viewModelScope.launch {
        if (cloudMode is FileModeState.StarBoardDrive) {
            val authority = cloudMode.authority
            StarBoardDriveClient.getSpaceData(authority)
                .onSuccess { updateStorage(it, cloudMode) }
                .onFailure { it.printStackTrace() }
        }
    }

    private fun updateStorage(spaceData: SpaceData, cloudMode: FileModeState) {
        val storages = Settings.STORAGES.valueCompat.toList()
        if (cloudMode is FileModeState.StarBoardDrive) {
            storages.firstOrNull { it is StarBoardDriveServer }?.let {
                val currentStorage = it as StarBoardDriveServer
                val storage = StarBoardDriveServer(
                    currentStorage.id,
                    currentStorage.customName,
                    currentStorage.authority.copy(
                        totalSpace = spaceData.total,
                        freeSpace = spaceData.remaining
                    )
                )
                Storages.replace(storage)
            }
        }
        if (cloudMode is FileModeState.GoogleDrive) {
            storages.firstOrNull { it is GoogleDriveServer }?.let {
                val currentStorage = it as GoogleDriveServer
                val storage = GoogleDriveServer(
                    currentStorage.id,
                    currentStorage.customName,
                    currentStorage.authority.copy(
                        totalSpace = spaceData.total,
                        usedSpace = spaceData.used
                    )
                )
                Storages.replace(storage)
            }
        }
        if (cloudMode is FileModeState.OneDrive) {
            storages.firstOrNull { it is OneDriveServer }?.let {
                val currentStorage = it as OneDriveServer
                val storage = OneDriveServer(
                    currentStorage.id,
                    currentStorage.customName,
                    currentStorage.authority.copy(
                        totalSpace = spaceData.total,
                        usedSpace = spaceData.used
                    )
                )
                Storages.replace(storage)
            }
        }
    }

    fun downloadToLocal(
        file: CloudFileItem,
        path: Path,
        isMove: Boolean,
        cloudMode: FileModeState,
        context: Context
    ) {
        Log.d("chengluo", "downloadToLocal file: $file path: $path isMove: $isMove")
        if (cloudMode is FileModeState.StarBoardDrive) {
            downloadFile(file, path, isMove, cloudMode, context)
        }
        if (cloudMode is FileModeState.GoogleDrive) {
            downloadGoogleDriveFile(file, path, isMove, cloudMode, context)
        }
        if (cloudMode is FileModeState.OneDrive) {
            downloadOneDriveFile(file, path, isMove, cloudMode, context)
        }
    }

    suspend fun uploadFolderRecursively(
        path: Path,
        parentDirectoryId: String,
        isMove: Boolean,
        cloudMode: FileModeState,
        topPath: Path,
        context: Context
    ) {
        Log.d("chengluo", "uploadFolderRecursively parentDirectoryId: $parentDirectoryId")
        if (path.isArchivePath) {
            _errorMessage.value = context.getString(R.string.cannot_extract_to_cloud_drive)
            return
        }
        if (path.isDirectory()) {
            val folderName = path.fileName.toString()
            if (cloudMode is FileModeState.StarBoardDrive) {
                val response = StarBoardDriveClient.createDirectory(
                    folderName,
                    parentDirectoryId,
                    cloudMode.authority
                )
                val newDirectoryId = if (response is ApiOperation.Success) {
                    refreshCloud()
                    response.data.id
                } else null

                Log.d("chengluo", "StarBoardDrive createDirectory id: $newDirectoryId")
                if (newDirectoryId == null) {
                    Log.e("chengluo", "Failed to create remote folder: $folderName")
                    return
                }
                try {
                    path.newDirectoryStream().use { directoryStream ->
                        for (child in directoryStream) {
                            uploadFolderRecursively(
                                child,
                                newDirectoryId,
                                isMove,
                                cloudMode,
                                topPath,
                                context
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("chengluo", "StarBoardDrive uploadFolderRecursively: ", e)
                }
            }
            if (cloudMode is FileModeState.GoogleDrive) {
                val newDirectoryId = if (cloudMode.authority.accessToken.isEmpty()) {
                    val result = GoogleDriveServiceClient.createDirectory(
                        folderName,
                        parentDirectoryId,
                        cloudMode.authority
                    )
                    refreshCloud()
                    result
                } else {
                    val response = GoogleDriveClient.createDirectory(
                        folderName,
                        parentDirectoryId,
                        cloudMode.authority
                    )
                    if (response is ApiOperation.Success) {
                        refreshCloud()
                        response.data
                    } else null
                }
                Log.d("chengluo", "GoogleDrive createDirectory id: $newDirectoryId")
                if (newDirectoryId == null) {
                    Log.e("chengluo", "Failed to create remote folder: $folderName")
                    return
                }
                try {
                    path.newDirectoryStream().use { directoryStream ->
                        for (child in directoryStream) {
                            uploadFolderRecursively(
                                child,
                                newDirectoryId,
                                isMove,
                                cloudMode,
                                topPath,
                                context
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("chengluo", "GoogleDrive uploadFolderRecursively: ", e)
                }
            }
            if (cloudMode is FileModeState.OneDrive) {
                val response = OneDriveClient.createDirectory(
                    folderName,
                    parentDirectoryId,
                    cloudMode.authority
                )
                val newDirectoryId = if (response is ApiOperation.Success) {
                    refreshCloud()
                    response.data.id
                } else null
                if (newDirectoryId == null) {
                    Log.e("chengluo", "Failed to create remote folder: $folderName")
                    return
                }

                try {
                    path.newDirectoryStream().use { directoryStream ->
                        for (child in directoryStream) {
                            uploadFolderRecursively(
                                child,
                                newDirectoryId,
                                isMove,
                                cloudMode,
                                topPath,
                                context
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("chengluo", "OneDrive uploadFolderRecursively: ", e)
                }
            }
        } else {
            uploadFile(path, parentDirectoryId, isMove, cloudMode, topPath, context)
        }
    }

    fun uploadFile(
        localFile: Path,
        directoryId: String,
        isMove: Boolean,
        cloudMode: FileModeState,
        topPath: Path,
        context: Context
    ) {
        if (cloudMode is FileModeState.StarBoardDrive) {
            uploadCloudFile(localFile, directoryId, isMove, cloudMode, topPath, context)
        }
        if (cloudMode is FileModeState.GoogleDrive) {
            uploadGoogleDriveFile(localFile, directoryId, isMove, cloudMode, topPath, context)
        }
        if (cloudMode is FileModeState.OneDrive) {
            uploadOneDriveFile(localFile, directoryId, isMove, cloudMode, topPath, context)
        }
    }

    private fun deleteEmptyDirectories(topPath: Path) {
        if (!topPath.exists() || !topPath.isDirectory()) return

        // 先处理子目录
        topPath.newDirectoryStream().use { directoryStream ->
            for (child in directoryStream) {
                if (child.isDirectory()) {
                    deleteEmptyDirectories(child) // 递归处理子目录
                }
            }
        }

        // 处理当前目录
        if (topPath.isEmptyDirectory()) {
            try {
                topPath.deleteIfExists()
                Log.d("chengluo", "deleteEmptyDirectory: ${topPath.name}")
            } catch (e: Exception) {
                Log.e("chengluo", "deleteEmptyDirectory failed: ${topPath.name}", e)
            }
        }
    }

    private fun uploadCloudFile(
        localFile: Path,
        directoryId: String,
        isMove: Boolean,
        starBoardDriveMode: FileModeState.StarBoardDrive,
        topPath: Path,
        context: Context
    ) {
        val id = localFile.toString().hashCode()
        val job = viewModelScope.launch {
            val name = localFile.fileName.toString()

            _fileTransferState.emit(
                FileTransfer(
                    id = id,
                    title = name,
                    text = context.getString(R.string.uploading),
                    max = 100,
                    progress = 0,
                    indeterminate = false,
                    status = FileTransfer.Status.TRANSFERRING
                )
            )

            val sizeInKB = localFile.size() / 1024
            StarBoardDriveClient.uploadChunks(localFile, starBoardDriveMode.authority)
                .collect { status ->
                    Log.d("chengluo", "uploadFile status: $status")
                    val transfer = when (status) {
                        is TransferStatus.Failed -> FileTransfer(
                            id = id,
                            title = name,
                            text = context.getString(R.string.upload_failed),
                            max = 100,
                            progress = 0,
                            indeterminate = false,
                            status = FileTransfer.Status.FAILED
                        )

                        is TransferStatus.Finished -> {
                            val url = status.data ?: return@collect
                            StarBoardDriveClient.uploadFileInfo(
                                name,
                                sizeInKB,
                                url,
                                directoryId,
                                starBoardDriveMode.authority
                            ).onSuccess {
                                refreshCloud()
                                getDriveSpace(starBoardDriveMode)
                                if (isMove) {
                                    try {
                                        localFile.deleteIfExists()
                                        deleteEmptyDirectories(topPath)
                                    } catch (e: Exception) {
                                        Log.e("chengluo", "uploadCloudFile delete: ", e)
                                    }
                                }
                            }.onFailure {
                                Log.e(
                                    "chengluo",
                                    "uploadCloudFile uploadFileInfo error: ",
                                    it
                                )
                            }
                            FileTransfer(
                                id = id,
                                title = name,
                                text = context.getString(R.string.upload_completed),
                                max = 100,
                                progress = 100,
                                indeterminate = false,
                                status = FileTransfer.Status.FINISHED
                            )
                        }

                        is TransferStatus.Transferring -> {
                            val sizeString =
                                status.total.asFileSize().formatHumanReadable(context)
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
        }
        TransferJobManager.register(id, job)
    }

    private fun uploadGoogleDriveFile(
        localFile: Path,
        directoryId: String,
        isMove: Boolean,
        cloudMode: FileModeState.GoogleDrive,
        topPath: Path,
        context: Context
    ) {
        val id = localFile.toString().hashCode()

        val job = viewModelScope.launch(Dispatchers.IO) {
            val name = localFile.fileName.toString()
            _fileTransferState.emit(
                FileTransfer(
                    id = id,
                    title = name,
                    text = context.getString(R.string.uploading),
                    max = 100,
                    progress = 0,
                    indeterminate = false,
                    status = FileTransfer.Status.TRANSFERRING
                )
            )

            if (cloudMode.authority.accessToken.isEmpty()) {
                GoogleDriveServiceClient.uploadFile(localFile, directoryId, cloudMode.authority)
                    ?.collect {
                        googleUploadTransfer(
                            id,
                            it,
                            localFile,
                            name,
                            isMove,
                            topPath,
                            context
                        )
                    }
            } else {
                GoogleDriveClient.uploadFile(localFile, directoryId, cloudMode.authority)
                    .collect {
                        googleUploadTransfer(
                            id,
                            it,
                            localFile,
                            name,
                            isMove,
                            topPath,
                            context
                        )
                    }
            }
        }
        TransferJobManager.register(id, job)
    }

    private suspend fun googleUploadTransfer(
        id: Int,
        status: TransferStatus,
        localFile: Path,
        name: String,
        isMove: Boolean,
        topPath: Path,
        context: Context
    ) {
        Log.d("chengluo", "googleUploadTransfer: $status")
        val transfer = when (status) {
            is TransferStatus.Failed -> FileTransfer(
                id = id,
                title = name,
                text = context.getString(R.string.upload_failed),
                max = 100,
                progress = 0,
                indeterminate = false,
                status = FileTransfer.Status.FAILED
            )

            is TransferStatus.Finished -> {
                if (isMove) {
                    try {
                        localFile.deleteIfExists()
                        deleteEmptyDirectories(topPath)
                    } catch (e: Exception) {
                        Log.e("chengluo", "googleUploadTransfer delete: ", e)
                    }
                }
                FileTransfer(
                    id = id,
                    title = name,
                    text = context.getString(R.string.upload_completed),
                    max = 100,
                    progress = 100,
                    indeterminate = false,
                    status = FileTransfer.Status.FINISHED
                )
            }

            is TransferStatus.Transferring -> {
                val sizeString =
                    status.total.asFileSize().formatHumanReadable(context)
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

    private fun uploadOneDriveFile(
        localFile: Path,
        directoryId: String,
        isMove: Boolean,
        cloudMode: FileModeState.OneDrive,
        topPath: Path,
        context: Context
    ) {
        val id = localFile.toString().hashCode()

        val job = viewModelScope.launch {
            val name = localFile.fileName.toString()
            _fileTransferState.emit(
                FileTransfer(
                    id = id,
                    title = name,
                    text = context.getString(R.string.uploading),
                    max = 100,
                    progress = 0,
                    indeterminate = false,
                    status = FileTransfer.Status.TRANSFERRING
                )
            )
            try {
                OneDriveClient.uploadFile(localFile, directoryId, cloudMode.authority)
                    .collect { status ->
                        Log.d("chengluo", "uploadFile status: $status")
                        val transfer = when (status) {
                            is TransferStatus.Failed -> FileTransfer(
                                id = id,
                                title = name,
                                text = context.getString(R.string.upload_failed),
                                max = 100,
                                progress = 0,
                                indeterminate = false,
                                status = FileTransfer.Status.FAILED
                            )

                            is TransferStatus.Finished -> {
                                if (isMove) {
                                    try {
                                        localFile.deleteIfExists()
                                        deleteEmptyDirectories(topPath)
                                    } catch (e: Exception) {
                                        Log.e("chengluo", "uploadOneDriveFile delete: ", e)
                                    }
                                }
                                FileTransfer(
                                    id = id,
                                    title = name,
                                    text = context.getString(R.string.upload_completed),
                                    max = 100,
                                    progress = 100,
                                    indeterminate = false,
                                    status = FileTransfer.Status.FINISHED
                                )
                            }

                            is TransferStatus.Transferring -> {
                                val sizeString =
                                    status.total.asFileSize().formatHumanReadable(context)
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
            } finally {
                TransferJobManager.cancel(id)
            }
        }
        TransferJobManager.register(id, job)
    }

    fun uploadFile(file: File, context: Context) = viewModelScope.launch {
        val directoryId = currentCloudPath?.directoryId ?: return@launch

        val cloudMode = currentFileMode

        if (cloudMode is FileModeState.StarBoardDrive) {

            val response = StarBoardDriveClient.uploadFile(file, cloudMode.authority)
            if (response is ApiOperation.Success) {

                val url = response.data.url ?: ""
                val sizeInKB = file.length() / 1024
                Log.d("chengluo", "uploadFile file size: $sizeInKB")

                StarBoardDriveClient.uploadFileInfo(
                    file.name,
                    sizeInKB,
                    url,
                    directoryId,
                    cloudMode.authority
                )
                    .onSuccess { refreshCloud() }
                    .onFailure { it.printStackTrace() }
            }
        }
        if (cloudMode is FileModeState.GoogleDrive) {
            if (cloudMode.authority.accessToken.isEmpty()) {
                GoogleDriveServiceClient.uploadFile(file, directoryId, cloudMode.authority)
                refreshCloud()
            } else {
                GoogleDriveClient.uploadFile(file, directoryId, cloudMode.authority)
                    .onSuccess { refreshCloud() }
                    .onFailure { it.printStackTrace() }
            }
        }
        if (cloudMode is FileModeState.OneDrive) {
            val id = file.toString().hashCode()
            val name = file.name
            OneDriveClient.uploadFile(file, directoryId, cloudMode.authority)
                .collect { status ->
                    Log.d("chengluo", "uploadFile status: $status")
                    val transfer = when (status) {
                        is TransferStatus.Failed -> FileTransfer(
                            id = id,
                            title = name,
                            text = context.getString(R.string.upload_failed),
                            max = 100,
                            progress = 0,
                            indeterminate = false,
                            status = FileTransfer.Status.FAILED
                        )

                        is TransferStatus.Finished -> {
                            FileTransfer(
                                id = id,
                                title = name,
                                text = context.getString(R.string.download_completed),
                                max = 100,
                                progress = 100,
                                indeterminate = false,
                                status = FileTransfer.Status.FINISHED
                            )
                        }

                        is TransferStatus.Transferring -> {
                            val sizeString =
                                status.total.asFileSize().formatHumanReadable(context)
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
        }
    }

    private val _fileTransferState = MutableSharedFlow<FileTransfer>()
    val fileTransferState: SharedFlow<FileTransfer> = _fileTransferState

    suspend fun downloadCloudRecursively(
        cloudFile: CloudFileItem,
        path: Path,
        isMove: Boolean,
        cloudMode: FileModeState,
        topCloudFile: CloudFileItem,
        context: Context
    ) {
        if (cloudFile.isDirectory) {
            val localDir = path.resolve(cloudFile.name)
            if (!localDir.exists()) {
                localDir.createDirectory()
                Log.d("chengluo", "localDir not exists: createDirectory first")
            }
            if (cloudMode is FileModeState.StarBoardDrive) {
                val response = StarBoardDriveClient.getFileList(cloudFile.id, cloudMode.authority)
                when (response) {
                    is ApiOperation.Success -> {
                        val children = response.data
                        for (child in children) {
                            downloadCloudRecursively(
                                cloudFile = child,
                                path = localDir,
                                isMove = isMove,
                                cloudMode = cloudMode,
                                topCloudFile = topCloudFile,
                                context = context
                            )
                        }
                        if (isMove) {
                            StarBoardDriveClient.delete(listOf(cloudFile.id), cloudMode.authority)
                                .onSuccess { refreshCloud() }
                            getDriveSpace(cloudMode)
                        }
                    }

                    is ApiOperation.Failure -> {
                        Log.e(
                            "chengluo",
                            "FileModeState.Cloud: Failed to list children for folder: ${cloudFile.name}",
                            response.exception
                        )
                    }
                }
            }
            if (cloudMode is FileModeState.GoogleDrive) {
                val response = if (cloudMode.authority.accessToken.isEmpty()) {
                    GoogleDriveServiceClient.getFileList(cloudFile.id, cloudMode.authority)
                } else {
                    GoogleDriveClient.getFileList(cloudFile.id, cloudMode.authority)
                }
                when (response) {
                    is ApiOperation.Success -> {
                        val children = response.data
                        for (child in children) {
                            downloadCloudRecursively(
                                cloudFile = child,
                                path = localDir,
                                isMove = isMove,
                                cloudMode = cloudMode,
                                topCloudFile = topCloudFile,
                                context = context
                            )
                        }
                        if (isMove) {
                            if (cloudMode.authority.accessToken.isEmpty()) {
                                GoogleDriveServiceClient.deleteEmptyFoldersRecursive(
                                    topCloudFile.id,
                                    cloudMode.authority
                                )
                                refreshCloud()
                            } else {
                                GoogleDriveClient.deleteEmptyFoldersRecursive(
                                    topCloudFile.id,
                                    cloudMode.authority
                                ).onSuccess { refreshCloud() }
                            }
                        }
                    }

                    is ApiOperation.Failure -> {
                        Log.e(
                            "chengluo",
                            "FileModeState.GoogleDrive: Failed to list children for folder: ${cloudFile.name}",
                            response.exception
                        )
                    }
                }
            }
            if (cloudMode is FileModeState.OneDrive) {
                val response = OneDriveClient.getFileList(cloudFile.id, cloudMode.authority)
                when (response) {
                    is ApiOperation.Success -> {
                        val children = response.data
                        for (child in children) {
                            downloadCloudRecursively(
                                cloudFile = child,
                                path = localDir,
                                isMove = isMove,
                                cloudMode = cloudMode,
                                topCloudFile = topCloudFile,
                                context = context
                            )
                        }
                        if (isMove) {
                            OneDriveClient.deleteEmptyFolderRecursive(
                                topCloudFile.id,
                                cloudMode.authority
                            ).onSuccess { refreshCloud() }
                        }
                    }

                    is ApiOperation.Failure -> {
                        Log.e(
                            "chengluo",
                            "FileModeState.OneDrive: Failed to list children for folder: ${cloudFile.name}",
                            response.exception
                        )
                    }
                }
            }
        } else {
            downloadToLocal(cloudFile, path, isMove, cloudMode, context)
        }
    }

    fun downloadFile(
        cloudFile: CloudFileItem,
        directoryPath: Path,
        isMove: Boolean,
        starBoardDriveMode: FileModeState.StarBoardDrive,
        context: Context
    ) = viewModelScope.launch {
        try {
            val id = cloudFile.hashCode()
            val authority = starBoardDriveMode.authority

            _fileTransferState.emit(
                FileTransfer(
                    id = id,
                    title = cloudFile.name,
                    text = context.getString(R.string.downloading),
                    max = 100,
                    progress = 0,
                    indeterminate = false,
                    status = FileTransfer.Status.TRANSFERRING
                )
            )
            val localFile = directoryPath.resolve(cloudFile.name)
            if (!localFile.exists()) localFile.createFile()

            if (localFile.size() >= cloudFile.size) {
                _fileTransferState.emit(
                    FileTransfer(
                        id = id,
                        title = cloudFile.name,
                        text = context.getString(R.string.download_completed),
                        max = 100,
                        progress = 100,
                        indeterminate = false,
                        status = FileTransfer.Status.FINISHED
                    )
                )
                return@launch
            }
            if (cloudFile.url == null) {
                Log.d("chengluo", "downloadFile: cloudFile.url == null return")
                return@launch
            }

            StarBoardDriveClient.downloadFile(cloudFile.url, localFile, authority)
                .collect { status ->
                    Log.d("chengluo", "downloadFile: $status")
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
                                deleteCloudFiles(cloudFileItemSetOf(cloudFile), starBoardDriveMode)
                            }
                            FileTransfer(
                                id = id,
                                title = cloudFile.name,
                                text = context.getString(R.string.download_completed),
                                max = 100,
                                progress = 100,
                                indeterminate = false,
                                status = FileTransfer.Status.FINISHED
                            )
                        }

                        is TransferStatus.Failed -> FileTransfer(
                            id = id,
                            title = cloudFile.name,
                            text = context.getString(R.string.download_failed),
                            max = 100,
                            progress = 0,
                            indeterminate = false,
                            status = FileTransfer.Status.FAILED
                        )
                    }
                    _fileTransferState.emit(transfer)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            _fileTransferState.emit(
                FileTransfer(
                    id = cloudFile.hashCode(),
                    title = cloudFile.name,
                    text = context.getString(R.string.download_failed),
                    max = 100,
                    progress = 0,
                    indeterminate = false,
                    status = FileTransfer.Status.FAILED
                )
            )
        }
    }

    fun downloadGoogleDriveFile(
        cloudFile: CloudFileItem,
        directoryPath: Path,
        isMove: Boolean,
        cloudMode: FileModeState.GoogleDrive,
        context: Context
    ) = viewModelScope.launch {
        try {
            val id = cloudFile.hashCode()

            _fileTransferState.emit(
                FileTransfer(
                    id = id,
                    title = cloudFile.name,
                    text = context.getString(R.string.downloading),
                    max = 100,
                    progress = 0,
                    indeterminate = false,
                    status = FileTransfer.Status.TRANSFERRING
                )
            )
            val localFile = directoryPath.resolve(cloudFile.name)
            if (!localFile.exists()) localFile.createFile()

            if (localFile.size() >= cloudFile.size) {
                _fileTransferState.emit(
                    FileTransfer(
                        id = id,
                        title = cloudFile.name,
                        text = context.getString(R.string.download_completed),
                        max = 100,
                        progress = 100,
                        indeterminate = false,
                        status = FileTransfer.Status.FINISHED
                    )
                )
                return@launch
            }

            if (cloudMode.authority.accessToken.isEmpty()) {
                GoogleDriveServiceClient.downloadFileWithResume(
                    cloudFile.id,
                    localFile,
                    cloudFile.size,
                    cloudMode.authority
                )?.collect {
                    googleDownloadTransfer(
                        id,
                        it,
                        cloudFile,
                        isMove,
                        cloudMode,
                        context
                    )
                }
            } else {
                GoogleDriveClient.downloadFile(
                    cloudFile.id,
                    cloudFile.size,
                    localFile,
                    cloudMode.authority
                ).collect {
                    googleDownloadTransfer(
                        id,
                        it,
                        cloudFile,
                        isMove,
                        cloudMode,
                        context
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            _fileTransferState.emit(
                FileTransfer(
                    id = cloudFile.hashCode(),
                    title = cloudFile.name,
                    text = context.getString(R.string.download_failed),
                    max = 100,
                    progress = 0,
                    indeterminate = false,
                    status = FileTransfer.Status.FAILED
                )
            )
        }
    }

    private suspend fun googleDownloadTransfer(
        id: Int,
        status: TransferStatus,
        cloudFile: CloudFileItem,
        isMove: Boolean,
        googleDriveMode: FileModeState.GoogleDrive,
        context: Context
    ) {
        Log.d("chengluo", "googleDownloadTransfer: $status")
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
                    if (googleDriveMode.authority.accessToken.isEmpty()) {
                        GoogleDriveServiceClient.deleteFile(cloudFile.id, googleDriveMode.authority)
                    } else {
                        GoogleDriveClient.deleteFileById(cloudFile.id, googleDriveMode.authority)
                    }
                }
                FileTransfer(
                    id = id,
                    title = cloudFile.name,
                    text = context.getString(R.string.download_completed),
                    max = 100,
                    progress = 100,
                    indeterminate = false,
                    status = FileTransfer.Status.FINISHED
                )
            }

            is TransferStatus.Failed -> FileTransfer(
                id = id,
                title = cloudFile.name,
                text = context.getString(R.string.download_failed),
                max = 100,
                progress = 0,
                indeterminate = false,
                status = FileTransfer.Status.FAILED
            )
        }
        _fileTransferState.emit(transfer)
    }

    fun downloadOneDriveFile(
        cloudFile: CloudFileItem,
        directoryPath: Path,
        isMove: Boolean,
        cloudMode: FileModeState.OneDrive,
        context: Context
    ) = viewModelScope.launch {
        try {
            val id = cloudFile.hashCode()
            _fileTransferState.emit(
                FileTransfer(
                    id = id,
                    title = cloudFile.name,
                    text = context.getString(R.string.downloading),
                    max = 100,
                    progress = 0,
                    indeterminate = false,
                    status = FileTransfer.Status.TRANSFERRING
                )
            )
            val localFile = directoryPath.resolve(cloudFile.name)
            if (!localFile.exists()) localFile.createFile()

            if (localFile.size() >= cloudFile.size) {
                _fileTransferState.emit(
                    FileTransfer(
                        id = id,
                        title = cloudFile.name,
                        text = context.getString(R.string.download_completed),
                        max = 100,
                        progress = 100,
                        indeterminate = false,
                        status = FileTransfer.Status.FINISHED
                    )
                )
                Log.d("chengluo", "downloadOneDriveFile localFile.size() >= cloudFile.size")
                return@launch
            }

            val response = OneDriveClient.getDownloadUrl(cloudFile.id, cloudMode.authority)
            if (response is ApiOperation.Failure) {
                Log.d("chengluo", "downloadOneDriveFile getDownloadUrl Failure: $response")
                return@launch
            }

            val url = (response as ApiOperation.Success).data
            OneDriveClient.downloadFile(url, cloudFile.size, localFile, cloudMode.authority)
                .collect { status ->
                    Log.d("chengluo", "downloadOneDriveFile: $status")
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
                                OneDriveClient.deleteFileById(cloudFile.id, cloudMode.authority)
                            }
                            FileTransfer(
                                id = id,
                                title = cloudFile.name,
                                text = context.getString(R.string.download_completed),
                                max = 100,
                                progress = 100,
                                indeterminate = false,
                                status = FileTransfer.Status.FINISHED
                            )
                        }

                        is TransferStatus.Failed -> FileTransfer(
                            id = id,
                            title = cloudFile.name,
                            text = context.getString(R.string.download_failed),
                            max = 100,
                            progress = 0,
                            indeterminate = false,
                            status = FileTransfer.Status.FAILED
                        )
                    }
                    _fileTransferState.emit(transfer)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            _fileTransferState.emit(
                FileTransfer(
                    id = cloudFile.hashCode(),
                    title = cloudFile.name,
                    text = context.getString(R.string.download_failed),
                    max = 100,
                    progress = 0,
                    indeterminate = false,
                    status = FileTransfer.Status.FAILED
                )
            )
        }
    }

    fun pasteFiles(context: Context) {
        Log.d("chengluo", "pasteFiles: ")
        if (isCloudFileMode) {
            val targetCloudPath = currentCloudPath ?: return
            val cloudMode = currentFileMode
            pasteToCloud(targetCloudPath, cloudMode, context)
        } else {
            val targetLocalDir = currentPath
            pasteToLocal(targetLocalDir, context)
        }
    }

    fun pasteToLocal(targetLocalDir: Path, context: Context) {
        Log.d("chengluo", "pasteToLocal: $targetLocalDir")
        val cloudFiles = cloudPasteState.files
        val cloudMode = cloudPasteState.cloudMode
        val isMove = !cloudPasteState.copy
        Log.d("chengluo", "pasteToLocal cloudMode: $cloudMode")
        if (cloudMode is FileModeState.StarBoardDrive ||
            cloudMode is FileModeState.OneDrive ||
            cloudMode is FileModeState.GoogleDrive
        ) {
            Log.d("chengluo", "pasteToLocal isMove: $isMove")
            cloudFiles.forEach { cloudFile ->
                viewModelScope.launch {
                    downloadCloudRecursively(
                        cloudFile = cloudFile,
                        path = targetLocalDir,
                        isMove = isMove,
                        cloudMode = cloudMode,
                        topCloudFile = cloudFile,
                        context = context
                    )
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

    fun pasteToCloud(targetCloudPath: CloudPathData, cloudMode: FileModeState, context: Context) {
        Log.d("chengluo", "pasteToCloud: $targetCloudPath")
        val cloudFiles = cloudPasteState.files
        if (cloudPasteState.copy) {
            copyCloudFiles(cloudFiles, targetCloudPath, cloudMode)
        } else {
            moveCloudFiles(cloudFiles, targetCloudPath, cloudMode)
        }
        clearCloudPasteState()

        val files = pasteState.files
        val directoryId = targetCloudPath.directoryId
        val isMove = !pasteState.copy

        files.forEach { file ->
            viewModelScope.launch {
                uploadFolderRecursively(
                    file.path,
                    directoryId,
                    isMove,
                    cloudMode,
                    file.path,
                    context
                )
            }
        }
        clearPasteState()
    }

    private fun moveCloudFiles(
        files: CloudFileItemSet,
        cloudPath: CloudPathData,
        cloudMode: FileModeState
    ) = viewModelScope.launch {
        if (files.isEmpty()) return@launch
        if (cloudMode is FileModeState.StarBoardDrive) {
            StarBoardDriveClient.moveFile(
                files.map { it.id },
                cloudPath.directoryId,
                cloudMode.authority
            )
                .onSuccess { refreshCloud() }
                .onFailure { it.printStackTrace() }
        }
        if (cloudMode is FileModeState.GoogleDrive) {
            files.forEach { file ->
                viewModelScope.launch {
                    if (cloudMode.authority.accessToken.isEmpty()) {
                        GoogleDriveServiceClient.moveFile(
                            file.id,
                            cloudPath.directoryId,
                            cloudMode.authority
                        )
                        refreshCloud()
                    } else {
                        GoogleDriveClient.moveFile(
                            file.id,
                            cloudPath.directoryId,
                            file.parentId,
                            cloudMode.authority
                        ).onSuccess { refreshCloud() }.onFailure { it.printStackTrace() }
                    }
                }
            }
        }
        if (cloudMode is FileModeState.OneDrive) {
            files.forEach { file ->
                viewModelScope.launch {
                    OneDriveClient.moveFile(file.id, cloudPath.directoryId, cloudMode.authority)
                        .onSuccess { refreshCloud() }
                        .onFailure { it.printStackTrace() }
                }
            }
        }
    }

    private fun copyCloudFiles(
        files: CloudFileItemSet,
        cloudPath: CloudPathData,
        cloudMode: FileModeState
    ) = viewModelScope.launch {
        Log.d("chengluo", "copyCloudFiles files size: ${files.size}")
        if (files.isEmpty()) return@launch
        if (cloudMode is FileModeState.StarBoardDrive) {
            StarBoardDriveClient.copyFile(
                files.map { it.id },
                cloudPath.directoryId,
                cloudMode.authority
            ).onSuccess { refreshCloud() }.onFailure { it.printStackTrace() }
        }
        if (cloudMode is FileModeState.GoogleDrive) {
            files.forEach { file ->
                viewModelScope.launch {
                    if (cloudMode.authority.accessToken.isEmpty()) {
                        GoogleDriveServiceClient.copyFile(
                            file.id,
                            cloudPath.directoryId,
                            file.isDirectory,
                            cloudMode.authority
                        )
                        refreshCloud()
                    } else {
                        GoogleDriveClient.copyFile(
                            file.id,
                            cloudPath.directoryId,
                            cloudMode.authority
                        ).onSuccess { refreshCloud() }.onFailure { it.printStackTrace() }
                    }
                }
            }
        }
        if (cloudMode is FileModeState.OneDrive) {
            files.forEach { file ->
                viewModelScope.launch {
                    OneDriveClient.copyFile(file.id, cloudPath.directoryId, cloudMode.authority)
                        .onSuccess { refreshCloud() }
                        .onFailure { it.printStackTrace() }
                }
            }
        }
    }

    fun reload() {
        if (isCloudFileMode) {
            refreshCloud()
        } else {
            val path = currentPath
            if (path.isArchivePath) {
                path.archiveRefresh()
            }
            _fileListLiveData.reload()
        }
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
        var changed = false
        if (cloudPasteState.copy != copy) {
            changed = cloudPasteState.files.isNotEmpty()
            cloudPasteState.files.clear()
            cloudPasteState.copy = copy
        }
        changed = changed or cloudPasteState.files.addAll(files)
        cloudPasteState.cloudMode = currentFileMode
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
        cloudPasteState.cloudMode = null
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

                    is FileModeState.GoogleDrive,
                    is FileModeState.OneDrive,
                    is FileModeState.StarBoardDrive -> return
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
