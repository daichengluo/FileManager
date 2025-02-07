package com.android.imagic.files.provider.sftp.client

interface Authenticator {
    fun getAuthentication(authority: Authority): Authentication?
}
