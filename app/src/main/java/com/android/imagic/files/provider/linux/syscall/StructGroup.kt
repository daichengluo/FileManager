package com.android.imagic.files.provider.linux.syscall

import com.android.imagic.files.provider.common.ByteString

class StructGroup(
    val gr_name: ByteString?,
    val gr_passwd: ByteString?,
    val gr_gid: Int,
    val gr_mem: Array<ByteString>?
)
