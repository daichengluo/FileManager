/*
 * Copyright (c) 2020 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package com.android.imagic.files.util

fun Any.hash(vararg values: Any?): Int = values.contentDeepHashCode()
