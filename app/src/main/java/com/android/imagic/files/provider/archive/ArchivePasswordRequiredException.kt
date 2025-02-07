package com.android.imagic.files.provider.archive

import android.content.Context
import com.android.imagic.files.fileaction.ArchivePasswordDialogActivity
import com.android.imagic.files.fileaction.ArchivePasswordDialogFragment
import com.android.imagic.files.provider.common.UserAction
import com.android.imagic.files.provider.common.UserActionRequiredException
import com.android.imagic.files.util.createIntent
import com.android.imagic.files.util.putArgs
import java8.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ArchivePasswordRequiredException(
    private val file: Path,
    reason: String?
) :
    UserActionRequiredException(file.toString(), null, reason) {

    override fun getUserAction(continuation: Continuation<Boolean>, context: Context): UserAction {
        return UserAction(
            ArchivePasswordDialogActivity::class.createIntent().putArgs(
                ArchivePasswordDialogFragment.Args(file) { continuation.resume(it) }
            ), ArchivePasswordDialogFragment.getTitle(context),
            ArchivePasswordDialogFragment.getMessage(file, context)
        )
    }
}
