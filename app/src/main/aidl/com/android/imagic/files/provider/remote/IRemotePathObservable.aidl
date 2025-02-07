package com.android.imagic.files.provider.remote;

import com.android.imagic.files.provider.remote.ParcelableException;
import com.android.imagic.files.util.RemoteCallback;

interface IRemotePathObservable {
    void addObserver(in RemoteCallback observer);

    void close(out ParcelableException exception);
}
