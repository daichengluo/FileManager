package com.android.imagic.files.navigation

import androidx.lifecycle.MediatorLiveData
import com.android.imagic.files.settings.Settings

object QuickAccessItemListLiveData : MediatorLiveData<List<NavigationItem?>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(Settings.QUICK_ACCESS) { loadValue() }
    }

    private fun loadValue() {
        value = navigationQuickAccessItems
    }
}
