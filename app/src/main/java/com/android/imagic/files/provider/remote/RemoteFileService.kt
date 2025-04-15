package com.android.imagic.files.provider.remote

import com.android.imagic.files.provider.common.PosixFileAttributeView
import com.android.imagic.files.provider.common.PosixFileStore
import java8.nio.file.FileSystem

abstract class RemoteFileService(private val remoteInterface: RemoteInterface<IRemoteFileService>) {
    @Throws(RemoteFileSystemException::class)
    fun getRemoteFileSystemProviderInterface(scheme: String): IRemoteFileSystemProvider =
        remoteInterface.get().call { getRemoteFileSystemProviderInterface(scheme) }

    @Throws(RemoteFileSystemException::class)
    fun getRemoteFileSystemInterface(fileSystem: FileSystem): IRemoteFileSystem =
        remoteInterface.get().call { getRemoteFileSystemInterface(fileSystem.toParcelable()) }

    @Throws(RemoteFileSystemException::class)
    fun getRemotePosixFileStoreInterface(fileStore: PosixFileStore): IRemotePosixFileStore =
        remoteInterface.get().call { getRemotePosixFileStoreInterface(fileStore.toParcelable()) }

    @Throws(RemoteFileSystemException::class)
    fun getRemotePosixFileAttributeViewInterface(
        attributeView: PosixFileAttributeView
    ): IRemotePosixFileAttributeView = remoteInterface.get().call {
        getRemotePosixFileAttributeViewInterface(attributeView.toParcelable())
    }
}
