package com.android.imagic.files.storage

import com.android.imagic.files.provider.cloud.sdrive.client.Authentication
import com.android.imagic.files.provider.cloud.sdrive.client.Authenticator
import com.android.imagic.files.provider.cloud.sdrive.client.Authority
import com.android.imagic.files.provider.cloud.sdrive.client.SDriveAuthentication

object SDriveServerAuthenticator : Authenticator {
    private val transientServers = mutableSetOf<SDriveServer>()

    override fun getAuthentication(authority: Authority): Authentication = SDriveAuthentication

    fun addTransientServer(server: SDriveServer) {
        synchronized(transientServers) { transientServers += server }
    }

    fun removeTransientServer(server: SDriveServer) {
        synchronized(transientServers) { transientServers -= server }
    }
}
