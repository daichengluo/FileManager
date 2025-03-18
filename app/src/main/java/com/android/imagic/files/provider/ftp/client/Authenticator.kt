
package com.android.imagic.files.provider.ftp.client

interface Authenticator {
    fun getPassword(authority: Authority): String?
}
