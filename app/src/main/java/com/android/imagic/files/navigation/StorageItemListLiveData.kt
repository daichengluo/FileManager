package com.android.imagic.files.navigation

import androidx.lifecycle.MediatorLiveData
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.storage.StorageVolumeListLiveData

object StorageItemListLiveData : MediatorLiveData<List<NavigationItem?>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(Settings.STORAGES) { loadValue() }
        addSource(StorageVolumeListLiveData) { loadValue() }
    }

    private fun loadValue() {
        value = navigationStorageItems
    }
}
