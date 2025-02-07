package com.android.imagic.files.provider.webdav.client

interface Authenticator {
    fun getAuthentication(authority: Authority): Authentication?
}
