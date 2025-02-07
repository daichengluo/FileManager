package com.android.imagic.files.coil

import android.content.Context
import android.content.pm.ApplicationInfo
import coil.key.Keyer
import coil.request.Options
import com.android.imagic.files.R
import com.android.imagic.files.compat.longVersionCodeCompat
import com.android.imagic.files.util.getDimensionPixelSize
import me.zhanghai.android.appiconloader.AppIconLoader
import java.io.Closeable

class AppIconApplicationInfoKeyer : Keyer<ApplicationInfo> {
    override fun key(data: ApplicationInfo, options: Options): String =
        AppIconLoader.getIconKey(data, data.longVersionCodeCompat, options.context)
}

class AppIconApplicationInfoFetcherFactory(
    context: Context
) : AppIconFetcher.Factory<ApplicationInfo>(
    // This is used by PrincipalListAdapter.
    context.getDimensionPixelSize(R.dimen.icon_size), context
) {
    override fun getApplicationInfo(data: ApplicationInfo): Pair<ApplicationInfo, Closeable?> =
        data to null
}
