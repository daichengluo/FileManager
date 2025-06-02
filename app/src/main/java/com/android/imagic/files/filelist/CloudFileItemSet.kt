package com.android.imagic.files.filelist

import android.os.Parcel
import android.os.Parcelable
import com.android.imagic.files.compat.writeParcelableListCompat
import com.android.imagic.files.file.cloud.CloudFileItem
import com.android.imagic.files.util.LinkedMapSet
import com.android.imagic.files.util.readParcelableListCompat

class CloudFileItemSet() : LinkedMapSet<Int, CloudFileItem>(CloudFileItem::id), Parcelable {
    constructor(parcel: Parcel) : this() {
        addAll(parcel.readParcelableListCompat())
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelableListCompat(toList(), flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CloudFileItemSet> {
        override fun createFromParcel(parcel: Parcel): CloudFileItemSet = CloudFileItemSet(parcel)

        override fun newArray(size: Int): Array<CloudFileItemSet?> = arrayOfNulls(size)
    }
}

fun cloudFileItemSetOf(vararg files: CloudFileItem) = CloudFileItemSet().apply { addAll(files) }
