package com.android.imagic.files.util

fun Any.hash(vararg values: Any?): Int = values.contentDeepHashCode()
