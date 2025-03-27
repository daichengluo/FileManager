package com.android.imagic.files.navigation

import androidx.lifecycle.MediatorLiveData
import com.android.imagic.files.settings.Settings

object QuickAccessesLiveData : MediatorLiveData<List<QuickAccess>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(Settings.QUICK_ACCESS_SETTINGS) { loadValue() }
    }

    private fun loadValue() {
        value = quickAccesses
    }
}
