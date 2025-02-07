package com.android.imagic.files.provider.smb

import com.android.imagic.files.provider.smb.client.Authority
import java8.nio.file.Path

fun Authority.createSmbRootPath(): Path =
    SmbFileSystemProvider.getOrNewFileSystem(this).rootDirectory
