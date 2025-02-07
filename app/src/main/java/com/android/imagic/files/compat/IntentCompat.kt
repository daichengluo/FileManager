/*
 * Copyright (c) 2021 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package com.android.imagic.files.compat

import android.content.Intent
import com.android.imagic.files.util.andInv

fun Intent.removeFlagsCompat(flags: Int) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        removeFlags(flags)
    } else {
        setFlags(this.flags andInv flags)
    }
}
