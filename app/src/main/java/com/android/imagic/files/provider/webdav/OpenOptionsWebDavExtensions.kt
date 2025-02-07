package com.android.imagic.files.provider.webdav

import com.android.imagic.files.provider.common.OpenOptions
import java8.nio.file.StandardOpenOption

internal fun OpenOptions.checkForWebDav() {
    if (deleteOnClose) {
        throw UnsupportedOperationException(StandardOpenOption.DELETE_ON_CLOSE.toString())
    }
    if (sync) {
        throw UnsupportedOperationException(StandardOpenOption.SYNC.toString())
    }
    if (dsync) {
        throw UnsupportedOperationException(StandardOpenOption.DSYNC.toString())
    }
}
