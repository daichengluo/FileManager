package com.android.imagic.files.viewer.video

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.fragment.app.commit
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.util.putArgs
import java.io.File

class VideoPlayerActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val intent = intent
            val uri = intent.data
            val name = intent.getStringExtra(EXTRA_FILE_NAME) ?: getFileName(uri!!)
            if (uri == null) finishAndRemoveTask()
            val fragment =
                VideoPlayerFragment().putArgs(VideoPlayerFragment.Args(intent, uri!!, name))
            supportFragmentManager.commit { add(android.R.id.content, fragment) }
        }
    }

    private fun getFileName(uri: Uri): String {
        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1 && cursor.moveToFirst()) {
                        return cursor.getString(index)
                    }
                }
                ""
            }

            ContentResolver.SCHEME_FILE -> File(uri.path ?: "").name
            else -> ""
        }
    }

    companion object {
        private val EXTRA_FILE_NAME = "${VideoPlayerActivity::class.java.name}.extra.FILENAME"

        fun putExtras(intent: Intent, name: String) {
            intent.putExtra(EXTRA_FILE_NAME, name)
        }
    }
}
