package com.android.imagic.files.provider.sftp

import com.android.imagic.files.provider.sftp.client.Authority
import java8.nio.file.Path

fun Authority.createSftpRootPath(): Path =
    SftpFileSystemProvider.getOrNewFileSystem(this).rootDirectory
