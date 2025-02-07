package com.android.imagic.files.util

import android.os.storage.StorageVolume
import com.android.imagic.files.compat.directoryCompat

val StorageVolume.isMounted: Boolean
    get() = directoryCompat != null
