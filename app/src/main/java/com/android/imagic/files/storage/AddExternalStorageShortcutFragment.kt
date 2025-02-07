/*
 * Copyright (c) 2023 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package com.android.imagic.files.storage

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.app.packageManager
import com.android.imagic.files.file.ExternalStorageUri
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.args
import com.android.imagic.files.util.createDocumentsUiViewDirectoryIntent
import com.android.imagic.files.util.finish
import com.android.imagic.files.util.showToast
import kotlinx.parcelize.Parcelize

class AddExternalStorageShortcutFragment : Fragment() {
    private val args by args<Args>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val uri = args.uri
        val hasDocumentsUi = uri.value.createDocumentsUiViewDirectoryIntent()
            .resolveActivity(packageManager) != null
        if (hasDocumentsUi) {
            val externalStorageShortcut = ExternalStorageShortcut(
                null, args.customNameRes?.let { getString(it) }, uri
            )
            Storages.addOrReplace(externalStorageShortcut)
        } else {
            showToast(R.string.activity_not_found)
        }
        finish()
    }

    @Parcelize
    class Args(
        @StringRes val customNameRes: Int?,
        val uri: ExternalStorageUri
    ) : ParcelableArgs
}
