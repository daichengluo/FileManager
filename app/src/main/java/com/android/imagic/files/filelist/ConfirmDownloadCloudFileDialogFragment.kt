package com.android.imagic.files.filelist

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.file.cloud.sdrive.CloudFileItem
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.args
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.show
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.parcelize.Parcelize

class ConfirmDownloadCloudFileDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    private val listener: Listener
        get() = requireParentFragment() as Listener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val file = args.file
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setMessage(R.string.cloud_file_download_dialog_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> listener.downloadToLocal(file) }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    companion object {
        fun show(file: CloudFileItem, fragment: Fragment) {
            ConfirmDownloadCloudFileDialogFragment().putArgs(Args(file)).show(fragment)
        }
    }

    @Parcelize
    class Args(val file: CloudFileItem) : ParcelableArgs

    interface Listener {
        fun downloadToLocal(file: CloudFileItem)
    }
}
