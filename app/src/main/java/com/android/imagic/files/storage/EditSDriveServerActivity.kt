package com.android.imagic.files.storage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.util.args
import com.android.imagic.files.util.putArgs

class EditSDriveServerActivity : AppActivity() {
    private val args by args<EditSDriveServerFragment.Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val fragment = EditSDriveServerFragment().putArgs(args)
            supportFragmentManager.commit { add(android.R.id.content, fragment) }
        }
    }
}
