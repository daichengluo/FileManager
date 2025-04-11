package com.android.imagic.files.filelist

import android.content.Context
import android.os.AsyncTask
import android.provider.MediaStore
import com.android.imagic.files.compat.pathCompat
import com.android.imagic.files.file.FileItem
import com.android.imagic.files.file.FileTypeCategory
import com.android.imagic.files.file.StorageEventReceiver
import com.android.imagic.files.file.getQueryInfo
import com.android.imagic.files.file.loadFileItem
import com.android.imagic.files.provider.common.list
import com.android.imagic.files.storage.StorageVolumeListLiveData
import com.android.imagic.files.util.CloseableLiveData
import com.android.imagic.files.util.Failure
import com.android.imagic.files.util.Loading
import com.android.imagic.files.util.Stateful
import com.android.imagic.files.util.Success
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path
import java8.nio.file.Paths
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class CategoryFileListLiveData(
    private val context: Context,
    private val fileTypeCategory: FileTypeCategory
) : CloseableLiveData<Stateful<List<FileItem>>>() {

    private var future: Future<Unit>? = null
    private val storageEventReceiver = StorageEventReceiver { onStorageChanged() }

    @Volatile
    private var isChangedWhileInactive = false

    init {
        loadValue()
        storageEventReceiver.register(context)
    }

    fun loadValue() {
        future?.cancel(true)
        value = Loading(value?.value)

        future = (AsyncTask.THREAD_POOL_EXECUTOR as ExecutorService).submit<Unit> {
            val fileList = mutableListOf<FileItem>()

            try {
                val resolver = context.contentResolver
                val (uri, projection, selection, selectionArgs) = fileTypeCategory.getQueryInfo()

                resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

                    while (cursor.moveToNext()) {
                        try {
                            val filePath = cursor.getString(pathIndex)
                            val path = Paths.get(filePath)
                            fileList.add(path.loadFileItem())
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }

                postValue(Success(fileList))
            } catch (e: Exception) {
                e.printStackTrace()
                postValue(Failure(valueCompat.value, e))
            }
        }
    }

    private fun queryMediaStore(
        context: Context,
        fileTypeCategory: FileTypeCategory
    ): List<FileItem> {
        val resolver = context.contentResolver
        val fileList = mutableListOf<FileItem>()
        val (uri, projection, selection, selectionArgs) = fileTypeCategory.getQueryInfo()

        resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                try {
                    val filePath = cursor.getString(pathIndex)
                    val path = Paths.get(filePath)
                    fileList.add(path.loadFileItem())
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return fileList
    }

    private fun queryExternalStorage(fileTypeCategory: FileTypeCategory): List<FileItem> {
        val fileList = mutableListOf<FileItem>()
        val storagePaths = StorageVolumeListLiveData.valueCompat
            .filter { it.isRemovable }
            .map { Paths.get(it.pathCompat) }

        for (storagePath in storagePaths) {
            storagePath.list(fileTypeCategory, INTERVAL_MILLIS) { paths: List<Path> ->
                for (path in paths) {
                    try {
                        fileList.add(path.loadFileItem())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return fileList
    }

    private fun onStorageChanged() {
        if (hasActiveObservers()) {
            loadValue()
        } else {
            isChangedWhileInactive = true
        }
    }

    override fun onActive() {
        if (isChangedWhileInactive) {
            loadValue()
            isChangedWhileInactive = false
        }
    }

    override fun close() {
        storageEventReceiver.unregister(context)
        future?.cancel(true)
    }

    companion object {
        private const val INTERVAL_MILLIS = 500L
    }
}
