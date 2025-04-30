package com.android.imagic.files.viewer.image

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.util.extraUrlStringList
import com.android.imagic.files.util.putArgs

class CloudImageViewerActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val intent = intent
            val position = intent.getIntExtra(EXTRA_POSITION, 0)
            val fragment = CloudImageViewerFragment().putArgs(
                CloudImageViewerFragment.Args(intent, position)
            )
            supportFragmentManager.commit { add(android.R.id.content, fragment) }
        }
    }

    companion object {
        private val EXTRA_POSITION = "${CloudImageViewerActivity::class.java.name}.extra.POSITION"

        fun putExtras(intent: Intent, paths: List<String>, position: Int) {
            // All extra put here must be framework classes, or we may crash the resolver activity.
            intent.extraUrlStringList = paths
            intent.putExtra(EXTRA_POSITION, position)
        }
    }
}
