package com.android.imagic.files.viewer.video

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.util.putArgs

class VideoPlayerActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val intent = intent
            val uri = intent.data
            if (uri == null) finishAndRemoveTask()
            val fragment = VideoPlayerFragment().putArgs(VideoPlayerFragment.Args(intent, uri!!))
            supportFragmentManager.commit { add(android.R.id.content, fragment) }
        }
    }
}
