package com.android.imagic.files.compat

import android.app.Service
import androidx.core.app.ServiceCompat

fun Service.stopForegroundCompat(flags: Int) {
    ServiceCompat.stopForeground(this, flags)
}
