package com.android.imagic.files.storage

import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import com.android.imagic.files.R
import com.android.imagic.files.file.asExternalStorageUri
import com.android.imagic.files.provider.document.resolver.ExternalStorageProviderHacks
import com.android.imagic.files.util.createIntent
import com.android.imagic.files.util.finish
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.startActivitySafe
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddStorageDialogFragment : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(R.string.storage_add_storage_title)
            .apply {
                val items = STORAGE_TYPES.map { getString(it.first) }.toTypedArray<CharSequence>()
                setItems(items) { _, which ->
                    startActivitySafe(STORAGE_TYPES[which].second)
                    finish()
                }
            }
            .create()

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        finish()
    }

    companion object {
        private val STORAGE_TYPES = listOfNotNull(
            R.string.storage_add_storage_ftp_server to
                    EditFtpServerActivity::class.createIntent()
                        .putArgs(EditFtpServerFragment.Args()),
            R.string.storage_add_storage_sftp_server to
                    EditSftpServerActivity::class.createIntent()
                        .putArgs(EditSftpServerFragment.Args()),
            R.string.storage_add_storage_smb_server to
                    AddLanSmbServerActivity::class.createIntent(),
            R.string.storage_add_storage_webdav_server to
                    EditWebDavServerActivity::class.createIntent()
                        .putArgs(EditWebDavServerFragment.Args()),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                R.string.storage_add_storage_android_data to
                        AddExternalStorageShortcutActivity::class.createIntent().putArgs(
                            AddExternalStorageShortcutFragment.Args(
                                R.string.storage_add_storage_android_data,
                                ExternalStorageProviderHacks.DOCUMENT_URI_ANDROID_DATA
                                    .asExternalStorageUri()
                            )
                        )
            } else null
        )
    }
}
