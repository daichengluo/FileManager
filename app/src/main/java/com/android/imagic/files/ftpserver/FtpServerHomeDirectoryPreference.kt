package com.android.imagic.files.ftpserver

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.android.imagic.files.settings.PathPreference
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path

class FtpServerHomeDirectoryPreference : PathPreference {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int,
        @StyleRes defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override var persistedPath: Path
        get() = Settings.FTP_SERVER_HOME_DIRECTORY.valueCompat
        set(value) {
            Settings.FTP_SERVER_HOME_DIRECTORY.putValue(value)
        }
}
