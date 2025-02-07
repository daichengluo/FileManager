package com.android.imagic.files.provider.sftp

import com.android.imagic.files.provider.common.PosixFileModeBit
import com.android.imagic.files.provider.common.toInt
import net.schmizz.sshj.sftp.FileAttributes

fun Set<PosixFileModeBit>.toSftpAttributes(): FileAttributes =
    FileAttributes.Builder().withPermissions(toInt()).build()
