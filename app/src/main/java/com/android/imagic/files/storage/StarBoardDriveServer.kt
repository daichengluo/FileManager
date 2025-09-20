package com.android.imagic.files.storage

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import com.android.imagic.files.R
import com.android.imagic.files.cloud.CloudLogoutDialogActivity
import com.android.imagic.files.cloud.CloudLogoutFragment
import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.util.createIntent
import com.android.imagic.files.util.putArgs
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@Parcelize
class StarBoardDriveServer(
    override val id: Long,
    override val customName: String?,
    val authority: Authority
) : Storage() {

    constructor(
        id: Long?,
        customName: String?,
        authority: Authority
    ) : this(id ?: Random.nextLong(), customName, authority)

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.computer_icon_white_25dp

    override fun getDefaultName(context: Context): String =
        context.getString(R.string.starboard_drive)

    override val description: String
        get() = authority.username

    override val path: Path?
        get() = null

    override fun createEditIntent(): Intent =
        CloudLogoutDialogActivity::class.createIntent().putArgs(CloudLogoutFragment.Args(this))
}
