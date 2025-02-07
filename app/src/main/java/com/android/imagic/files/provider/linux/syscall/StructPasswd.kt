package com.android.imagic.files.provider.linux.syscall

import com.android.imagic.files.provider.common.ByteString

class StructPasswd(
    val pw_name: ByteString?,
    val pw_uid: Int,
    val pw_gid: Int,
    val pw_gecos: ByteString?,
    val pw_dir: ByteString?,
    val pw_shell: ByteString?
)
