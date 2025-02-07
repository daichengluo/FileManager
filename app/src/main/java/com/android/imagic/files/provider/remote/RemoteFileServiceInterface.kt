package com.android.imagic.files.provider.remote

import com.android.imagic.files.provider.FileSystemProviders

open class RemoteFileServiceInterface :
    com.android.imagic.files.provider.remote.IRemoteFileService.Stub() {
    override fun getRemoteFileSystemProviderInterface(scheme: String): com.android.imagic.files.provider.remote.IRemoteFileSystemProvider =
        RemoteFileSystemProviderInterface(FileSystemProviders[scheme])

    override fun getRemoteFileSystemInterface(fileSystem: ParcelableObject): com.android.imagic.files.provider.remote.IRemoteFileSystem =
        RemoteFileSystemInterface(fileSystem.value())

    override fun getRemotePosixFileStoreInterface(
        fileStore: ParcelableObject
    ): com.android.imagic.files.provider.remote.IRemotePosixFileStore =
        RemotePosixFileStoreInterface(fileStore.value())

    override fun getRemotePosixFileAttributeViewInterface(
        attributeView: ParcelableObject
    ): com.android.imagic.files.provider.remote.IRemotePosixFileAttributeView =
        RemotePosixFileAttributeViewInterface(attributeView.value())
}
