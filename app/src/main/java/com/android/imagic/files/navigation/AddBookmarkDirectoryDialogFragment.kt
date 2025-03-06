package com.android.imagic.files.navigation

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import com.android.imagic.files.R
import com.android.imagic.files.databinding.AddBookmarkDirectoryDialogBinding
import com.android.imagic.files.filelist.toUserFriendlyString
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.ParcelableParceler
import com.android.imagic.files.util.ParcelableState
import com.android.imagic.files.util.args
import com.android.imagic.files.util.finish
import com.android.imagic.files.util.getState
import com.android.imagic.files.util.layoutInflater
import com.android.imagic.files.util.putState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

class AddBookmarkDirectoryDialogFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    private lateinit var path: Path

    private lateinit var binding: AddBookmarkDirectoryDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        path = savedInstanceState?.getState<State>()?.path ?: args.bookmarkDirectory.path
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.navigation_edit_bookmark_directory_title)
            .apply {
                binding = AddBookmarkDirectoryDialogBinding.inflate(context.layoutInflater)
                val bookmarkDirectory = args.bookmarkDirectory
                binding.nameLayout.placeholderText = bookmarkDirectory.defaultName
                updatePathText()
                setView(binding.root)
            }
            .setPositiveButton(android.R.string.ok) { _, _ -> save() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putState(State(path))
    }

    private fun updatePathText() {
        binding.pathText.setText(path.toUserFriendlyString())
    }

    private fun save() {
        val customName = binding.nameEdit.text.toString()
            .takeIf { it.isNotEmpty() && it != binding.nameLayout.placeholderText }
        val bookmarkDirectory = args.bookmarkDirectory.copy(customName = customName, path = path)
        BookmarkDirectories.add(bookmarkDirectory)
        finish()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        finish()
    }

    @Parcelize
    class Args(val bookmarkDirectory: BookmarkDirectory) : ParcelableArgs

    @Parcelize
    private class State(var path: @WriteWith<ParcelableParceler> Path) : ParcelableState
}
