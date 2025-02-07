package com.android.imagic.files.fileproperties.permission

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.databinding.SetSelinuxContextDialogBinding
import com.android.imagic.files.file.FileItem
import com.android.imagic.files.filejob.FileJobService
import com.android.imagic.files.provider.common.PosixFileAttributes
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.args
import com.android.imagic.files.util.layoutInflater
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.show
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.parcelize.Parcelize

class SetSeLinuxContextDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    private lateinit var binding: SetSelinuxContextDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.file_properties_permission_set_selinux_context_title)
            .apply {
                binding = SetSelinuxContextDialogBinding.inflate(context.layoutInflater)
                if (savedInstanceState == null) {
                    binding.seLinuxContextEdit.setText(argsSeLinuxContext)
                }
                binding.recursiveCheck.isVisible = args.file.attributes.isDirectory
                setView(binding.root)
            }
            .setPositiveButton(android.R.string.ok) { _, _ -> setSeLinuxContext() }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(
                R.string.file_properties_permission_set_selinux_context_restore
            ) { _, _ -> restoreSeLinuxContext() }
            .create()
            .apply {
                window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            }

    private fun setSeLinuxContext() {
        val seLinuxContext = binding.seLinuxContextEdit.text.toString()
        val recursive = binding.recursiveCheck.isChecked
        if (!recursive) {
            if (seLinuxContext == argsSeLinuxContext) {
                return
            }
        }
        FileJobService.setSeLinuxContext(
            args.file.path, seLinuxContext, recursive, requireContext()
        )
    }

    private val argsSeLinuxContext: String
        get() {
            val attributes = args.file.attributes as PosixFileAttributes
            return attributes.seLinuxContext()?.toString().orEmpty()
        }

    private fun restoreSeLinuxContext() {
        val recursive = binding.recursiveCheck.isChecked
        FileJobService.restoreSeLinuxContext(args.file.path, recursive, requireContext())
    }

    companion object {
        fun show(file: FileItem, fragment: Fragment) {
            SetSeLinuxContextDialogFragment().putArgs(Args(file)).show(fragment)
        }
    }

    @Parcelize
    class Args(val file: FileItem) : ParcelableArgs
}
