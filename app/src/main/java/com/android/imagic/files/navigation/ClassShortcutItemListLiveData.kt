package com.android.imagic.files.navigation

import androidx.lifecycle.MediatorLiveData
import com.android.imagic.files.settings.Settings

object ClassShortcutItemListLiveData : MediatorLiveData<List<NavigationItem?>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(StandardDirectoriesLiveData) { loadValue() }
        addSource(Settings.BOOKMARK_DIRECTORIES) { loadValue() }
    }

    private fun loadValue() {
        value = navigationClassShortcutItems
    }
}
