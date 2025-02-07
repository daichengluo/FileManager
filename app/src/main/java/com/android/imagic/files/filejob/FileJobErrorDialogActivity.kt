package com.android.imagic.files.filejob

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.util.args
import com.android.imagic.files.util.putArgs

class FileJobErrorDialogActivity : AppActivity() {
    private val args by args<FileJobErrorDialogFragment.Args>()

    private lateinit var fragment: FileJobErrorDialogFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            fragment = FileJobErrorDialogFragment().putArgs(args)
            supportFragmentManager.commit {
                add(fragment, FileJobErrorDialogFragment::class.java.name)
            }
        } else {
            fragment = supportFragmentManager.findFragmentByTag(
                FileJobErrorDialogFragment::class.java.name
            ) as FileJobErrorDialogFragment
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            fragment.onFinish()
        }
    }
}
