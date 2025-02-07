package com.android.imagic.files.storage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import com.android.imagic.files.app.AppActivity

class AddDocumentTreeActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            supportFragmentManager.commit { add(android.R.id.content, AddDocumentTreeFragment()) }
        }
    }
}
