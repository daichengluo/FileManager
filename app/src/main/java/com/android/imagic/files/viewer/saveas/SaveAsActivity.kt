package com.android.imagic.files.viewer.saveas

import android.os.Bundle
import android.os.Environment
import com.android.imagic.files.R
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.asMimeTypeOrNull
import com.android.imagic.files.filejob.FileJobService
import com.android.imagic.files.filelist.FileListActivity
import com.android.imagic.files.util.saveAsPath
import com.android.imagic.files.util.showToast
import java8.nio.file.Path
import java8.nio.file.Paths

class SaveAsActivity : AppActivity() {
    private val createFileLauncher =
        registerForActivityResult(FileListActivity.CreateFileContract(), ::onCreateFileResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val mimeType = intent.type?.asMimeTypeOrNull() ?: MimeType.ANY
        val path = intent.saveAsPath
        if (path == null) {
            showToast(R.string.save_as_error)
            finish()
            return
        }
        val title = path.fileName.toString()
        val initialPath =
            Paths.get(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
            )
        createFileLauncher.launch(Triple(mimeType, title, initialPath))
    }

    private fun onCreateFileResult(result: Path?) {
        if (result == null) {
            finish()
            return
        }
        FileJobService.save(intent.saveAsPath!!, result, this)
        finish()
    }
}
