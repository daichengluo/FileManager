package com.android.imagic.files.navigation

import androidx.lifecycle.MediatorLiveData

object ClassShortcutItemListLiveData : MediatorLiveData<List<NavigationItem?>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(StandardDirectoriesLiveData) { loadValue() }
    }

    private fun loadValue() {
        value = navigationClassShortcutItems
    }
}
