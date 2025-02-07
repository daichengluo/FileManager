package com.android.imagic.files.filelist

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import com.android.imagic.files.R
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.asMimeType
import com.android.imagic.files.file.fileProviderUri
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.ParcelableParceler
import com.android.imagic.files.util.args
import com.android.imagic.files.util.createViewIntent
import com.android.imagic.files.util.extraPath
import com.android.imagic.files.util.finish
import com.android.imagic.files.util.startActivitySafe
import com.android.imagic.files.util.withChooser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

class OpenFileAsDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(getString(R.string.file_open_as_title_format, args.path.name))
            .apply {
                val items = FILE_TYPES.map { getString(it.first) }.toTypedArray<CharSequence>()
                setItems(items) { _, which -> openAs(FILE_TYPES[which].second) }
            }
            .create()

    private fun openAs(mimeType: MimeType) {
        val intent = args.path.fileProviderUri.createViewIntent(mimeType)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            .apply { extraPath = args.path }
            .withChooser()
        startActivitySafe(intent)
        finish()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        finish()
    }

    companion object {
        private val FILE_TYPES = listOf(
            R.string.file_open_as_type_text to "text/plain",
            R.string.file_open_as_type_image to "image/*",
            R.string.file_open_as_type_audio to "audio/*",
            R.string.file_open_as_type_video to "video/*",
            R.string.file_open_as_type_directory to MimeType.DIRECTORY.value,
            R.string.file_open_as_type_any to "*/*"
        ).map { it.first to it.second.asMimeType() }
    }

    @Parcelize
    class Args(val path: @WriteWith<ParcelableParceler> Path) : ParcelableArgs
}
