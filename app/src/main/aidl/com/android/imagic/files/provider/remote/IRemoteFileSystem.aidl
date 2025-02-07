package com.android.imagic.files.provider.remote;

import com.android.imagic.files.provider.remote.ParcelableException;

interface IRemoteFileSystem {
    void close(out ParcelableException exception);
}
