package com.android.imagic.files.navigation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.util.args
import com.android.imagic.files.util.putArgs

class AddBookmarkDirectoryDialogActivity : AppActivity() {
    private val args by args<AddBookmarkDirectoryDialogFragment.Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val fragment = AddBookmarkDirectoryDialogFragment().putArgs(args)
            supportFragmentManager.commit {
                add(fragment, AddBookmarkDirectoryDialogFragment::class.java.name)
            }
        }
    }
}
