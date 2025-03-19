package com.android.imagic.files.filelist

import android.os.AsyncTask
import com.android.imagic.files.compat.pathCompat
import com.android.imagic.files.file.FileItem
import com.android.imagic.files.file.MimeTypeCategory
import com.android.imagic.files.file.loadFileItem
import com.android.imagic.files.provider.common.list
import com.android.imagic.files.storage.StorageVolumeListLiveData
import com.android.imagic.files.util.CloseableLiveData
import com.android.imagic.files.util.Failure
import com.android.imagic.files.util.Loading
import com.android.imagic.files.util.Stateful
import com.android.imagic.files.util.Success
import com.android.imagic.files.util.isMounted
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path
import java8.nio.file.Paths
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

class CategoryFileListLiveData(
    private val category: MimeTypeCategory
) : CloseableLiveData<Stateful<List<FileItem>>>() {
    private var futures: MutableList<Future<Unit>> = mutableListOf()
    private val pendingTasks = AtomicInteger(0)

    init {
        loadValue()
    }

    fun loadValue() {
        futures.forEach { it.cancel(true) }
        value = Loading(emptyList())
        val fileList = CopyOnWriteArrayList<FileItem>()
        val storagePaths = StorageVolumeListLiveData.valueCompat
            .filter { it.isMounted }
            .map { Paths.get(it.pathCompat) }
        pendingTasks.set(storagePaths.size)

        for (storagePath in storagePaths) {
            val future = (AsyncTask.THREAD_POOL_EXECUTOR as ExecutorService).submit<Unit> {
                try {
                    storagePath.list(category, INTERVAL_MILLIS) { paths: List<Path> ->
                        for (path in paths) {
                            val fileItem = try {
                                path.loadFileItem()
                            } catch (e: IOException) {
                                e.printStackTrace()
                                continue
                            }
                            fileList.add(fileItem)
                        }
                        postValue(Loading(fileList.toList()))
                    }
                } catch (e: Exception) {
                    postValue(Failure(valueCompat.value, e))
                } finally {
                    if (pendingTasks.decrementAndGet() == 0) {
                        postValue(Success(fileList.toList()))
                    }
                }
            }
            futures.add(future)
        }
    }

    override fun close() {
        futures.forEach { it.cancel(true) }
    }

    companion object {
        private const val INTERVAL_MILLIS = 500L
    }
}
