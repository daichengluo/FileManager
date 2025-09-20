package com.android.imagic.files.cloud

import android.R
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.commit
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.util.args
import com.android.imagic.files.util.putArgs

class CloudLoginActivity : AppActivity() {
    private val args by args<CloudLoginFragment.Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setLayout(740, 760)
        val params = window.attributes
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 250
        window.attributes = params
        title = null

        // Calls ensureSubDecor().
        findViewById<View>(R.id.content)
        if (savedInstanceState == null) {
            val fragment = CloudLoginFragment().putArgs(args)
            supportFragmentManager.commit { add(R.id.content, fragment) }
        }
    }
}
