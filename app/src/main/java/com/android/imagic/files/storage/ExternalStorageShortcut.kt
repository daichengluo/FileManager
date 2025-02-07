package com.android.imagic.files.storage

import android.content.Context
import android.content.Intent
import com.android.imagic.files.file.ExternalStorageUri
import com.android.imagic.files.file.displayName
import com.android.imagic.files.util.createDocumentsUiViewDirectoryIntent
import com.android.imagic.files.util.createIntent
import com.android.imagic.files.util.putArgs
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@Parcelize
data class ExternalStorageShortcut(
    override val id: Long,
    override val customName: String?,
    val uri: ExternalStorageUri
) : Storage() {
    constructor(
        id: Long?,
        customName: String?,
        uri: ExternalStorageUri
    ) : this(id ?: Random.nextLong(), customName, uri)

    override fun getDefaultName(context: Context): String = uri.displayName

    override val description: String
        get() = uri.value.toString()

    override val path: Path?
        get() = null

    override fun createIntent(): Intent = uri.value.createDocumentsUiViewDirectoryIntent()

    override fun createEditIntent(): Intent =
        EditExternalStorageShortcutDialogActivity::class.createIntent()
            .putArgs(EditExternalStorageShortcutDialogFragment.Args(this))
}
