package com.android.imagic.files.storage

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import com.android.imagic.files.R
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.util.createIntent
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@Parcelize
class GoogleDriveServer(
    override val id: Long,
    override val customName: String?,
    val authority: ServerAuthority
) : Storage() {

    constructor(
        id: Long?,
        customName: String?,
        authority: ServerAuthority
    ) : this(id ?: Random.nextLong(), customName, authority)

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.nav_list_google_icon

    override fun getDefaultName(context: Context): String = context.getString(R.string.google_drive)

    override val description: String
        get() = "Google Drive"

    override val path: Path?
        get() = null

    override fun createEditIntent(): Intent = CloudLogoutDialogActivity::class.createIntent()
}
