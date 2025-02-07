package com.android.imagic.files.provider.remote;

import com.android.imagic.files.provider.remote.IRemoteFileSystem;
import com.android.imagic.files.provider.remote.IRemoteFileSystemProvider;
import com.android.imagic.files.provider.remote.IRemotePosixFileAttributeView;
import com.android.imagic.files.provider.remote.IRemotePosixFileStore;
import com.android.imagic.files.provider.remote.ParcelableObject;

interface IRemoteFileService {
    IRemoteFileSystemProvider getRemoteFileSystemProviderInterface(String scheme);

    IRemoteFileSystem getRemoteFileSystemInterface(in ParcelableObject fileSystem);

    IRemotePosixFileStore getRemotePosixFileStoreInterface(in ParcelableObject fileStore);

    IRemotePosixFileAttributeView getRemotePosixFileAttributeViewInterface(
        in ParcelableObject attributeView
    );
}
