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

class CopyOrMoveItselfDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    private val listener: Listener
        get() = requireParentFragment() as Listener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val isCopy = args.isCopy
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setMessage(
                if (isCopy) R.string.file_job_cannot_copy_into_itself_title
                else R.string.file_job_cannot_move_into_itself_title
            )
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    companion object {
        fun show(isCopy: Boolean, fragment: Fragment) {
            CopyOrMoveItselfDialogFragment().putArgs(Args(isCopy)).show(fragment)
        }
    }

    @Parcelize
    class Args(val isCopy: Boolean) : ParcelableArgs

    interface Listener {
        fun downloadToLocal(file: CloudFileItem)
    }
}
