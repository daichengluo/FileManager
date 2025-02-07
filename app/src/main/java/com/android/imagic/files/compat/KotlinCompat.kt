package com.android.imagic.files.compat

import kotlin.collections.removeFirst as kotlinRemoveFirst
import kotlin.collections.removeLast as kotlinRemoveLast
import kotlin.comparisons.reversed as kotlinReversed

fun <T> Comparator<T>.reversedCompat(): Comparator<T> = kotlinReversed()

fun <T> MutableList<T>.removeFirstCompat(): T = kotlinRemoveFirst()

fun <T> MutableList<T>.removeLastCompat(): T = kotlinRemoveLast()
