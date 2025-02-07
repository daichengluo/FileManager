package com.android.imagic.files.filelist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.android.imagic.files.settings.PathSettings
import com.android.imagic.files.settings.SettingLiveData
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path

class FileViewTypeLiveData(pathLiveData: LiveData<Path>) : MediatorLiveData<FileViewType>() {
    private lateinit var pathViewTypeLiveData: SettingLiveData<FileViewType?>

    private fun loadValue() {
        if (!this::pathViewTypeLiveData.isInitialized) {
            // Not yet initialized.
            return
        }
        val value = pathViewTypeLiveData.value ?: Settings.FILE_LIST_VIEW_TYPE.valueCompat
        if (this.value != value) {
            this.value = value
        }
    }

    fun putValue(value: FileViewType) {
        if (pathViewTypeLiveData.value != null) {
            pathViewTypeLiveData.putValue(value)
        } else {
            Settings.FILE_LIST_VIEW_TYPE.putValue(value)
        }
    }

    init {
        addSource(Settings.FILE_LIST_VIEW_TYPE) { loadValue() }
        addSource(pathLiveData) { path: Path ->
            if (this::pathViewTypeLiveData.isInitialized) {
                removeSource(pathViewTypeLiveData)
            }
            pathViewTypeLiveData = PathSettings.getFileListViewType(path)
            addSource(pathViewTypeLiveData) { loadValue() }
        }
    }
}
