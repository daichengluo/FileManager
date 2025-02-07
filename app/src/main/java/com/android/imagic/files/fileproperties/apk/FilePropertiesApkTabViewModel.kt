package com.android.imagic.files.fileproperties.apk

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.android.imagic.files.util.Stateful
import java8.nio.file.Path

class FilePropertiesApkTabViewModel(path: Path) : ViewModel() {
    private val _apkInfoLiveData = ApkInfoLiveData(path)
    val apkInfoLiveData: LiveData<Stateful<ApkInfo>>
        get() = _apkInfoLiveData

    fun reload() {
        _apkInfoLiveData.loadValue()
    }

    override fun onCleared() {
        _apkInfoLiveData.close()
    }
}
