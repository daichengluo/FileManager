/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package com.android.imagic.files.provider.archive

import android.os.Parcelable
import com.android.imagic.files.util.ParcelableParceler
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

@Parcelize
internal data class ArchiveFileKey(
    private val archiveFile: @WriteWith<ParcelableParceler> Path,
    private val entryName: String
) : Parcelable
