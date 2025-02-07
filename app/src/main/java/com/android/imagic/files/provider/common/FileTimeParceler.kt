package com.android.imagic.files.provider.common

import android.os.Parcel
import com.android.imagic.files.compat.readSerializableCompat
import java8.nio.file.attribute.FileTime
import kotlinx.parcelize.Parceler
import java.time.Instant

object FileTimeParceler : Parceler<FileTime?> {
    override fun create(parcel: Parcel): FileTime? =
        parcel.readSerializableCompat<Instant>()?.let { FileTime.from(it) }

    override fun FileTime?.write(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(this?.toInstant())
    }
}
