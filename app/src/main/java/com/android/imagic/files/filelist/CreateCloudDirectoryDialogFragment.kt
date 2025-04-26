package com.android.imagic.files.filelist

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.util.show

class CreateCloudDirectoryDialogFragment : FileNameDialogFragment() {
    override val listener: Listener
        get() = super.listener as Listener

    override fun isNameValid(name: String): Boolean {
        val containsEmoji = name.containsEmoji()
        val isNameValid = super.isNameValid(name)
        if (containsEmoji) {
            binding.nameLayout.error = getString(R.string.file_name_error_invalid)
        }
        return isNameValid && !containsEmoji
    }

    @StringRes
    override val titleRes: Int = R.string.file_create_directory_title

    override fun onOk(name: String) {
        listener.createCloudDirectory(name)
    }

    companion object {
        fun show(fragment: Fragment) {
            CreateCloudDirectoryDialogFragment().show(fragment)
        }
    }

    interface Listener : FileNameDialogFragment.Listener {
        fun createCloudDirectory(name: String)
    }
}

private fun String.containsEmoji(): Boolean {
    for (char in this) {
        val type = Character.getType(char)
        if (type == Character.SURROGATE.toInt() || type == Character.OTHER_SYMBOL.toInt()) {
            return true
        }
    }
    return false
}
