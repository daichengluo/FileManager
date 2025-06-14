package com.android.imagic.files.storage

import com.android.imagic.files.cloud.sdrive.client.Authentication
import com.android.imagic.files.cloud.sdrive.client.Authenticator
import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.cloud.sdrive.client.StarBoardDriveAuthentication

object StarBoardDriveServerAuthenticator : Authenticator {
    private val transientServers = mutableSetOf<StarBoardDriveServer>()

    override fun getAuthentication(authority: Authority): Authentication =
        StarBoardDriveAuthentication

    fun addTransientServer(server: StarBoardDriveServer) {
        synchronized(transientServers) { transientServers += server }
    }

    fun removeTransientServer(server: StarBoardDriveServer) {
        synchronized(transientServers) { transientServers -= server }
    }
}
