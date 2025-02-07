package com.android.imagic.files.filelist

import android.os.Bundle
import com.android.imagic.files.app.AppActivity
import com.android.imagic.files.file.MimeType
import com.android.imagic.files.file.fileProviderUri
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.ParcelableParceler
import com.android.imagic.files.util.args
import com.android.imagic.files.util.createEditIntent
import com.android.imagic.files.util.startActivitySafe
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

// Use a trampoline activity so that we can have a proper icon and title.
class EditFileActivity : AppActivity() {
    private val args by args<Args>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivitySafe(args.path.fileProviderUri.createEditIntent(args.mimeType))
        finish()
    }

    @Parcelize
    class Args(
        val path: @WriteWith<ParcelableParceler> Path,
        val mimeType: MimeType
    ) : ParcelableArgs
}
