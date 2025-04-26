package com.android.imagic.files.filelist

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.file.cloud.sdrive.CloudFileItem
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.args
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.show
import kotlinx.parcelize.Parcelize

class RenameCloudFileDialogFragment : FileNameDialogFragment() {
    private val args by args<Args>()

    override val listener: Listener
        get() = super.listener as Listener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        if (savedInstanceState == null) {
            binding.nameEdit.setSelection(0, args.file.name.length)
        }
        return dialog
    }

    @StringRes
    override val titleRes: Int = R.string.rename

    override val initialName: String?
        get() = args.file.name

    override fun onOk(name: String) {
        listener.renameCloudFile(args.file, name)
    }

    companion object {
        fun show(file: CloudFileItem, fragment: Fragment) {
            RenameCloudFileDialogFragment().putArgs(Args(file)).show(fragment)
        }
    }

    @Parcelize
    class Args(val file: CloudFileItem) : ParcelableArgs

    interface Listener : FileNameDialogFragment.Listener {
        fun renameCloudFile(file: CloudFileItem, newName: String)
    }
}
