package com.android.imagic.files.filelist

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.android.imagic.files.navigation.NavigationRootMapLiveData
import com.android.imagic.files.navigation.QuickAccessCategory
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path

class BreadcrumbLiveData(
    private val fileModeStateLiveData: LiveData<FileModeState>,
    private val trailLiveData: LiveData<TrailData>,
    private val cloudPathListLiveData: LiveData<List<CloudPathData>?>
) : MediatorLiveData<BreadcrumbData>() {
    init {
        addSource(fileModeStateLiveData) { loadValue() }
        addSource(trailLiveData) { loadValue() }
        addSource(cloudPathListLiveData) { loadValue() }
        addSource(NavigationRootMapLiveData) { loadValue() }
    }

    private fun loadValue() {
        val navigationRootMap = NavigationRootMapLiveData.valueCompat
        val fileModeState = fileModeStateLiveData.valueCompat
        val trailData = trailLiveData.valueCompat
        val cloudPathList = cloudPathListLiveData.valueCompat

        val paths = mutableListOf<Path>()
        val nameProducers = mutableListOf<(Context) -> String>()
        var selectedIndex = 0

        when (fileModeState) {
            FileModeState.Local -> {
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
                value = BreadcrumbData(paths, null, nameProducers, selectedIndex)
            }

            is FileModeState.QuickAccess -> {
                val quickAccessWrap = fileModeState.quickAccessWrap
                if (quickAccessWrap.quickAccess.category == QuickAccessCategory.DOWNLOAD) {
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
                    value = BreadcrumbData(paths, null, nameProducers, selectedIndex)
                } else {
                    nameProducers.add { quickAccessWrap.title }
                    paths.add(trailData.trail.last())
                    value = BreadcrumbData(paths, null, nameProducers, selectedIndex)
                }
            }

            is FileModeState.Cloud,
            is FileModeState.GoogleDrive,
            is FileModeState.OneDrive -> {
                if (cloudPathList != null) {
                    selectedIndex = cloudPathList.size - 1
                    value = BreadcrumbData(paths, cloudPathList, nameProducers, selectedIndex)
                }
            }
        }
    }
}
