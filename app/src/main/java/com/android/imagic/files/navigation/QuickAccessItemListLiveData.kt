package com.android.imagic.files.navigation

import androidx.lifecycle.MediatorLiveData
import com.android.imagic.files.settings.Settings

object QuickAccessItemListLiveData : MediatorLiveData<List<NavigationItem>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(Settings.STANDARD_DIRECTORY_SETTINGS) { loadValue() }
    }

    private fun loadValue() {
        value = quickAccessItems
    }
}