/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package com.android.imagic.files.provider.remote

import java8.nio.file.FileSystem
import com.android.imagic.files.provider.common.PosixFileAttributeView
import com.android.imagic.files.provider.common.PosixFileStore

abstract class RemoteFileService(private val remoteInterface: RemoteInterface<com.android.imagic.files.provider.remote.IRemoteFileService>) {
    @Throws(RemoteFileSystemException::class)
    fun getRemoteFileSystemProviderInterface(scheme: String): com.android.imagic.files.provider.remote.IRemoteFileSystemProvider =
        remoteInterface.get().call { getRemoteFileSystemProviderInterface(scheme) }

    @Throws(RemoteFileSystemException::class)
    fun getRemoteFileSystemInterface(fileSystem: FileSystem): com.android.imagic.files.provider.remote.IRemoteFileSystem =
        remoteInterface.get().call { getRemoteFileSystemInterface(fileSystem.toParcelable()) }

    @Throws(RemoteFileSystemException::class)
    fun getRemotePosixFileStoreInterface(fileStore: PosixFileStore): com.android.imagic.files.provider.remote.IRemotePosixFileStore =
        remoteInterface.get().call { getRemotePosixFileStoreInterface(fileStore.toParcelable()) }

    @Throws(RemoteFileSystemException::class)
    fun getRemotePosixFileAttributeViewInterface(
        attributeView: PosixFileAttributeView
    ): com.android.imagic.files.provider.remote.IRemotePosixFileAttributeView =
        remoteInterface.get().call {
            getRemotePosixFileAttributeViewInterface(attributeView.toParcelable())
        }
}
