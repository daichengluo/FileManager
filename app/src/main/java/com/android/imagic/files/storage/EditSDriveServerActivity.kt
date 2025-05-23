package com.android.imagic.files.storage

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.commit
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.util.putArgs

class EditSDriveServerActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setLayout(740, 760)
        val params = window.attributes
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 250
        window.attributes = params
        title = null

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val fragment = EditSDriveServerFragment().putArgs(EditSDriveServerFragment.Args(false))
            supportFragmentManager.commit { add(android.R.id.content, fragment) }
        }
    }
}
