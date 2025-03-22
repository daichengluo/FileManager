package com.android.imagic.files.navigation

import androidx.lifecycle.MediatorLiveData

object SettingsItemListLiveData : MediatorLiveData<List<NavigationItem>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
    }

    private fun loadValue() {
        value = navigationSettingsItems
    }
}
