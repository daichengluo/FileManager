package com.android.imagic.files.storage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.util.args
import com.android.imagic.files.util.putArgs

class EditDeviceStorageDialogActivity : AppActivity() {
    private val args by args<EditDeviceStorageDialogFragment.Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val fragment = EditDeviceStorageDialogFragment().putArgs(args)
            supportFragmentManager.commit {
                add(fragment, EditDeviceStorageDialogFragment::class.java.name)
            }
        }
    }
}
