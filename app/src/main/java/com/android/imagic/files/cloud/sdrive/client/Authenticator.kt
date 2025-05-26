package com.android.imagic.files.cloud.sdrive.client

interface Authenticator {
    fun getAuthentication(authority: Authority): Authentication?
}
