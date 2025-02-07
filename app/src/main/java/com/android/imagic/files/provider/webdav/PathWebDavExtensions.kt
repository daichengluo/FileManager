package com.android.imagic.files.provider.webdav

import com.android.imagic.files.provider.webdav.client.Authority
import java8.nio.file.Path

fun Authority.createWebDavRootPath(): Path =
    WebDavFileSystemProvider.getOrNewFileSystem(this).rootDirectory
