/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package com.android.imagic.files.navigation

import androidx.lifecycle.MediatorLiveData
import java8.nio.file.Path
import com.android.imagic.files.util.valueCompat

object NavigationRootMapLiveData : MediatorLiveData<Map<Path, NavigationRoot>>() {
    init {
        // Initialize value before we have any active observer.
        loadValue()
        addSource(NavigationItemListLiveData) { loadValue() }
    }

    private fun loadValue() {
        value = NavigationItemListLiveData.valueCompat
            .mapNotNull { it as? NavigationRoot }
            .associateBy { it.path }
    }
}
