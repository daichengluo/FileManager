package com.android.imagic.files.provider.smb.client

interface Authenticator {
    fun getPassword(authority: Authority): String?
}
