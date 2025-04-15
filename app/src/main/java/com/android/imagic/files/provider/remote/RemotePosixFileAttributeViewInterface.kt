package com.android.imagic.files.provider.remote

import com.android.imagic.files.provider.common.ParcelableFileTime
import com.android.imagic.files.provider.common.ParcelablePosixFileMode
import com.android.imagic.files.provider.common.PosixFileAttributeView
import com.android.imagic.files.provider.common.PosixGroup
import com.android.imagic.files.provider.common.PosixUser

class RemotePosixFileAttributeViewInterface(
    private val attributeView: PosixFileAttributeView
) : IRemotePosixFileAttributeView.Stub() {
    override fun readAttributes(exception: ParcelableException): ParcelableObject? =
        tryRun(exception) { attributeView.readAttributes().toParcelable() }

    override fun setTimes(
        lastModifiedTime: ParcelableFileTime?,
        lastAccessTime: ParcelableFileTime?,
        createTime: ParcelableFileTime?,
        exception: ParcelableException
    ) {
        tryRun(exception) {
            attributeView.setTimes(
                lastModifiedTime?.value, lastAccessTime?.value, createTime?.value
            )
        }
    }

    override fun setOwner(owner: PosixUser, exception: ParcelableException) {
        tryRun(exception) { attributeView.setOwner(owner) }
    }

    override fun setGroup(group: PosixGroup, exception: ParcelableException) {
        tryRun(exception) { attributeView.setGroup(group) }
    }

    override fun setMode(mode: ParcelablePosixFileMode, exception: ParcelableException) {
        tryRun(exception) { attributeView.setMode(mode.value) }
    }

    override fun setSeLinuxContext(context: ParcelableObject, exception: ParcelableException) {
        tryRun(exception) { attributeView.setSeLinuxContext(context.value()) }
    }

    override fun restoreSeLinuxContext(exception: ParcelableException) {
        tryRun(exception) { attributeView.restoreSeLinuxContext() }
    }
}
