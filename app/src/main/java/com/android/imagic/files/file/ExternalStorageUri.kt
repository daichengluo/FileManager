/*
 * Copyright (c) 2024 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package com.android.imagic.files.file

import android.net.Uri
import android.os.Parcelable
import android.provider.DocumentsContract
import com.android.imagic.files.compat.DocumentsContractCompat
import com.android.imagic.files.util.StableUriParceler
import com.android.imagic.files.util.takeIfNotEmpty
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

@Parcelize
@JvmInline
value class ExternalStorageUri(val value: @WriteWith<StableUriParceler> Uri) : Parcelable {
    constructor(
        rootId: String,
        path: String
    ) : this(
        DocumentsContract.buildDocumentUriUsingTree(
            DocumentsContract.buildTreeDocumentUri(
                DocumentsContractCompat.EXTERNAL_STORAGE_PROVIDER_AUTHORITY,
                rootId
            ),
            "$rootId:$path"
        )
    )

    val rootId: String
        get() = DocumentsContract.getTreeDocumentId(value)

    val path: String
        get() = DocumentsContract.getDocumentId(value).removePrefix("$rootId:")
}

fun Uri.asExternalStorageUriOrNull(): ExternalStorageUri? =
    if (isExternalStorageUri) ExternalStorageUri(this) else null

fun Uri.asExternalStorageUri(): ExternalStorageUri {
    require(isExternalStorageUri)
    return ExternalStorageUri(this)
}

/** @see DocumentsContractCompat.isDocumentUri */
private val Uri.isExternalStorageUri: Boolean
    get() =
        DocumentsContractCompat.isDocumentUri(this) &&
            authority == DocumentsContractCompat.EXTERNAL_STORAGE_PROVIDER_AUTHORITY &&
            pathSegments.size == 4

val ExternalStorageUri.displayName: String
    get() = path.takeLastWhile { it != '/' }.takeIfNotEmpty() ?: "/"
