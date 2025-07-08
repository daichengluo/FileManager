package com.android.imagic.files.filelist

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.args
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.show
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.parcelize.Parcelize

class ConfirmReplaceCloudFileDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    private val listener: Listener
        get() = requireParentFragment() as Listener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val file = args.file
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setMessage(getString(R.string.file_replace_message_format, file.name))
            .setPositiveButton(android.R.string.ok) { _, _ -> listener.replaceFile(file) }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    companion object {
        fun show(file: CloudFileItem, fragment: Fragment) {
            ConfirmReplaceCloudFileDialogFragment().putArgs(Args(file)).show(fragment)
        }
    }

    @Parcelize
    class Args(val file: CloudFileItem) : ParcelableArgs

    interface Listener {
        fun replaceFile(file: CloudFileItem)
    }
}
