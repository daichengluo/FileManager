package com.android.imagic.files.provider.remote

import java8.nio.file.FileSystem

class RemoteFileSystemInterface(private val fileSystem: FileSystem) :
    com.android.imagic.files.provider.remote.IRemoteFileSystem.Stub() {
    override fun close(exception: ParcelableException) {
        tryRun(exception) { fileSystem.close() }
    }
}
