/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package com.android.imagic.files.provider.linux.syscall

import com.android.imagic.files.provider.common.ByteString

class StructInotifyEvent(
    val wd: Int,
    val mask: Int, /* uint32_t */
    val cookie: Int, /* uint32_t */
    val name: ByteString?
)
