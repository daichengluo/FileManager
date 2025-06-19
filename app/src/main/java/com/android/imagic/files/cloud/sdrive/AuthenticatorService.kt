package com.android.imagic.files.cloud.sdrive

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AuthenticatorService : Service() {

    private lateinit var authenticator: StarBoardAccountAuthenticator

    override fun onCreate() {
        authenticator = StarBoardAccountAuthenticator(this)
    }

    override fun onBind(p0: Intent?): IBinder? = authenticator.iBinder
}