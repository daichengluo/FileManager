package com.android.imagic.files.fileproperties

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.cloud.CloudFileItem
import com.android.imagic.files.cloud.extension
import com.android.imagic.files.databinding.CloudFilePropertiesDialogBinding
import com.android.imagic.files.file.asFileSize
import com.android.imagic.files.file.formatShort
import com.android.imagic.files.file.getName
import com.android.imagic.files.fileproperties.FilePropertiesTabFragment.ViewBuilder
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.args
import com.android.imagic.files.util.getQuantityString
import com.android.imagic.files.util.layoutInflater
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.show
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.parcelize.Parcelize

class CloudFilePropertiesDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    private lateinit var binding: CloudFilePropertiesDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(getString(R.string.file_properties_title_format, args.file.name))
            .apply {
                binding = CloudFilePropertiesDialogBinding.inflate(context.layoutInflater)
                setView(binding.root)
            }
            .setPositiveButton(android.R.string.ok, null)
            .create()

    // HACK: Work around child FragmentManager requiring a view.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val file = args.file

        val builder = ViewBuilder(binding.linearLayout)

        with(builder) {
            addItemView(R.string.file_properties_basic_name, file.name)

            addItemView(R.string.file_properties_basic_type, getTypeText(file))

            if (file.isDirectory) {
                val count = file.childrenCount ?: 0
                addItemView(
                    R.string.file_properties_basic_contents, getDirectoryContentsText(count)
                )
            } else {
                addItemView(R.string.file_properties_basic_size, getSizeText(file))
            }

            val lastModificationTime = file.modifyTime?.formatShort(requireContext()) ?: ""
            addItemView(R.string.file_properties_basic_last_modification_time, lastModificationTime)

            file.url?.let {
                addItemView(R.string.cloud_file_properties_download_url, it)
            }

            build()
        }
    }

    private fun getTypeText(file: CloudFileItem): String = getString(
        R.string.file_properties_basic_type_format,
        file.mimeType.getName(file.extension, requireContext()),
        file.mimeType.value
    )

    private fun getDirectoryContentsText(count: Int): String =
        if (count == 0) {
            getString(R.string.empty)
        } else {
            getQuantityString(R.plurals.file_item_file_count_format, count, count)
        }

    private fun getSizeText(file: CloudFileItem): String {
        val size = file.size.asFileSize()
        val context = requireContext()
        val sizeInBytes = size.formatInBytes(context)
        return if (size.isHumanReadableInBytes) {
            sizeInBytes
        } else {
            val humanReadableSize = size.formatHumanReadable(context)
            getString(
                R.string.file_properties_basic_size_with_human_readable_format, humanReadableSize,
                sizeInBytes
            )
        }
    }

    override fun onStart() {
        super.onStart()

        // AlertDialog (its AlertController) adds FLAG_ALT_FOCUSABLE_IM when the initial custom
        // view doesn't have any view that returns true for onCheckIsTextEditor().
        requireDialog().window!!.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }

    companion object {
        fun show(file: CloudFileItem, fragment: Fragment) {
            CloudFilePropertiesDialogFragment().putArgs(Args(file)).show(fragment)
        }
    }

    @Parcelize
    class Args(val file: CloudFileItem) : ParcelableArgs
}
