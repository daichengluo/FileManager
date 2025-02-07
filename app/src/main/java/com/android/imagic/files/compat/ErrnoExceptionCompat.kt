package com.android.imagic.files.compat

import android.system.ErrnoException
import com.android.imagic.files.hiddenapi.RestrictedHiddenApi
import com.android.imagic.files.util.lazyReflectedField

@RestrictedHiddenApi
private val functionNameField by lazyReflectedField(ErrnoException::class.java, "functionName")

val ErrnoException.functionNameCompat: String
    get() = functionNameField.get(this) as String
