package com.android.imagic.files.cloud

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.android.imagic.files.BuildConfig
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.storage.SDriveServer
import com.android.imagic.files.util.valueCompat

class AuthProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = BuildConfig.AUTH_PROVIDIER_AUTHORITY
        val URI_AUTH = "content://$AUTHORITY/authority".toUri()
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        Log.d(
            "chengluo",
            "query uri: $uri projection: $projection selection: $selection selectionArgs: $selectionArgs sortOrder: $sortOrder"
        )
        if (uri == URI_AUTH) {
            val matrixCursor = MatrixCursor(
                arrayOf("openId", "accessToken", "userId", "username", "headerImageUrl")
            )
            val storages = Settings.STORAGES.valueCompat.toList()
            val sDriveServers = storages.filter { it is SDriveServer }
            sDriveServers.firstOrNull()?.let { server ->
                val sDriveServer = server as SDriveServer
                val authority = sDriveServer.authority
                Log.d("chengluo", "query authority: $authority")
                matrixCursor.addRow(
                    arrayOf(
                        authority.openId,
                        authority.accessToken,
                        authority.userId,
                        authority.username,
                        authority.headerImageUrl
                    )
                )
            }
            return matrixCursor
        }
        return null
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
}