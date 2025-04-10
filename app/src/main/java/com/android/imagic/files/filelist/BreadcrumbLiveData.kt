package com.android.imagic.files.filelist

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.android.imagic.files.navigation.NavigationRootMapLiveData
import com.android.imagic.files.navigation.QuickAccess
import com.android.imagic.files.navigation.getExternalStorageDirectory
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path
import java8.nio.file.Paths

class BreadcrumbLiveData(
    private val trailLiveData: LiveData<TrailData>,
    private val quickAccessLiveData: LiveData<QuickAccess?>
) : MediatorLiveData<BreadcrumbData>() {
    init {
        addSource(trailLiveData) { loadValue() }
        addSource(quickAccessLiveData) { loadValue() }
        addSource(NavigationRootMapLiveData) { loadValue() }
    }

    private fun loadValue() {
        val navigationRootMap = NavigationRootMapLiveData.valueCompat
        val trailData = trailLiveData.valueCompat
        val quickAccessData = quickAccessLiveData.valueCompat
        val paths = mutableListOf<Path>()
        val nameProducers = mutableListOf<(Context) -> String>()
        var selectedIndex = 0

        if (quickAccessData != null && quickAccessData.category != null) {
            nameProducers.add { quickAccessData.getTitle(it) }
            paths.add(Paths.get(getExternalStorageDirectory(quickAccessData.relativePath)))
        } else {
            selectedIndex = trailData.currentIndex
            for (path in trailData.trail) {
                val navigationRoot = navigationRootMap[path]
                val itemCount = nameProducers.size
                if (navigationRoot != null && selectedIndex >= itemCount) {
                    selectedIndex -= itemCount
                    paths.clear()
                    paths.add(navigationRoot.path)
                    nameProducers.clear()
                    nameProducers.add { navigationRoot.getName(it) }
                } else {
                    paths.add(path)
                    nameProducers.add { path.name }
                }
            }
        }

        value = BreadcrumbData(paths, nameProducers, selectedIndex)
    }
}
