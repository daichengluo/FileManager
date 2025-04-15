package com.android.imagic.files.provider.remote

import com.android.imagic.files.provider.common.ByteString
import com.android.imagic.files.provider.common.PosixFileAttributeView
import com.android.imagic.files.provider.common.PosixFileAttributes
import com.android.imagic.files.provider.common.PosixFileModeBit
import com.android.imagic.files.provider.common.PosixGroup
import com.android.imagic.files.provider.common.PosixUser
import com.android.imagic.files.provider.common.toParcelable
import java8.nio.file.attribute.FileTime
import java.io.IOException

abstract class RemotePosixFileAttributeView(
    private val remoteInterface: RemoteInterface<IRemotePosixFileAttributeView>
) : PosixFileAttributeView {
    @Throws(IOException::class)
    override fun readAttributes(): PosixFileAttributes =
        remoteInterface.get().call { exception -> readAttributes(exception) }.value()

    @Throws(IOException::class)
    override fun setTimes(
        lastModifiedTime: FileTime?,
        lastAccessTime: FileTime?,
        createTime: FileTime?
    ) {
        remoteInterface.get().call { exception ->
            setTimes(
                lastModifiedTime?.toParcelable(), lastAccessTime?.toParcelable(),
                createTime?.toParcelable(), exception
            )
        }
    }

    @Throws(IOException::class)
    override fun setOwner(owner: PosixUser) {
        remoteInterface.get().call { exception -> setOwner(owner, exception) }
    }

    @Throws(IOException::class)
    override fun setGroup(group: PosixGroup) {
        remoteInterface.get().call { exception -> setGroup(group, exception) }
    }

    @Throws(IOException::class)
    override fun setMode(mode: Set<PosixFileModeBit>) {
        remoteInterface.get().call { exception -> setMode(mode.toParcelable(), exception) }
    }

    @Throws(IOException::class)
    override fun setSeLinuxContext(context: ByteString) {
        remoteInterface.get().call { exception ->
            setSeLinuxContext(context.toParcelable(), exception)
        }
    }

    @Throws(IOException::class)
    override fun restoreSeLinuxContext() {
        remoteInterface.get().call { exception -> restoreSeLinuxContext(exception) }
    }
}
