package com.android.imagic.files.storage

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.android.imagic.files.R
import com.android.imagic.files.util.takeIfNotEmpty
import java8.nio.file.Path

abstract class Storage : Parcelable {
    abstract val id: Long

    open val iconRes: Int
        @DrawableRes
        get() = R.drawable.nav_storage_internal

    abstract val customName: String?

    abstract fun getDefaultName(context: Context): String

    fun getName(context: Context): String = customName?.takeIfNotEmpty() ?: getDefaultName(context)

    abstract val description: String

    abstract val path: Path?

    open val linuxPath: String? = null

    open val isVisible: Boolean = true

    open fun createIntent(): Intent? = null

    abstract fun createEditIntent(): Intent
}
