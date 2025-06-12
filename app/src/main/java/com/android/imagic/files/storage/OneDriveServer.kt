package com.android.imagic.files.storage

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import com.android.imagic.files.R
import com.android.imagic.files.util.createIntent
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@Parcelize
class OneDriveServer(
    override val id: Long,
    override val customName: String?,
    val accessToken: String
) : Storage() {

    constructor(
        id: Long?,
        customName: String?,
        accessToken: String
    ) : this(id ?: Random.nextLong(), customName, accessToken)

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.nav_list_one_icon

    override fun getDefaultName(context: Context): String = context.getString(R.string.one_drive)

    override val description: String
        get() = "One Drive"

    override val path: Path?
        get() = null

    override fun createEditIntent(): Intent = CloudLogoutDialogActivity::class.createIntent()
}
