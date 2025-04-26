package com.android.imagic.files.filelist

import android.content.Context
import java8.nio.file.Path

data class BreadcrumbData(
    val paths: List<Path>,
    val cloudPaths: List<CloudPathData>? = null,
    val nameProducers: List<(Context) -> String>,
    val selectedIndex: Int
)
