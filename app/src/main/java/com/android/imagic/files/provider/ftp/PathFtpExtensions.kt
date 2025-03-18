
package com.android.imagic.files.provider.ftp

import com.android.imagic.files.provider.ftp.client.Authority
import java8.nio.file.Path

fun Authority.createFtpRootPath(): Path =
    FtpFileSystemProvider.getOrNewFileSystem(this).rootDirectory
