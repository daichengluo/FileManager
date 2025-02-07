package com.android.imagic.files.fileproperties.checksum

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.android.imagic.files.util.Stateful
import java8.nio.file.Path

class FilePropertiesChecksumTabViewModel(path: Path) : ViewModel() {
    private val _checksumInfoLiveData = ChecksumInfoLiveData(path)
    val checksumInfoLiveData: LiveData<Stateful<ChecksumInfo>>
        get() = _checksumInfoLiveData

    fun reload() {
        _checksumInfoLiveData.loadValue()
    }

    override fun onCleared() {
        _checksumInfoLiveData.close()
    }
}
