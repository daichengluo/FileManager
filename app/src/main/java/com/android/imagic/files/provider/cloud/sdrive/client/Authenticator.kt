package com.android.imagic.files.provider.cloud.sdrive.client

interface Authenticator {
    fun getAuthentication(authority: Authority): Authentication?
}
