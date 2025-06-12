package com.android.imagic.files.storage

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.appwidget.LoginWidgetUpdater
import com.android.imagic.files.navigation.NavigationFragment
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.args
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.show
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.parcelize.Parcelize

class CloudLogoutFragment : AppCompatDialogFragment() {
    private val args by args<Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setMessage(getString(R.string.message_logout))
            .setPositiveButton(R.string.log_out) { _, _ ->
                remove()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create()
    }

    private fun remove() {
        tryLogoutIfNeeded()
        Storages.remove(args.server)
        LoginWidgetUpdater.updateWidgets(requireContext())
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        (activity as? CloudLogoutDialogActivity)?.finish()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        (activity as? CloudLogoutDialogActivity)?.finish()
    }

    private fun tryLogoutIfNeeded() {
        (parentFragment as? NavigationFragment)?.logout()
    }

    companion object {
        const val TAG = "logout"

        fun show(server: StarBoardDriveServer, fragment: Fragment) {
            CloudLogoutFragment().putArgs(Args(server)).show(fragment)
        }
    }

    @Parcelize
    class Args(val server: StarBoardDriveServer) : ParcelableArgs
}
