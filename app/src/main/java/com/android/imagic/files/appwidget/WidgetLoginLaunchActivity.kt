package com.android.imagic.files.appwidget

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.commit
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.storage.EditStarBoardDriveServerFragment
import com.android.imagic.files.util.putArgs

class WidgetLoginLaunchActivity : AppActivity() {

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
            val fragment = EditStarBoardDriveServerFragment().putArgs(
                EditStarBoardDriveServerFragment.Args(true)
            )
            supportFragmentManager.commit { add(android.R.id.content, fragment) }
        }
    }
}
