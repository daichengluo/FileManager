package com.android.imagic.files.provider.cloud.gdrive

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes

class GoogleDriveAuthHelper(private val context: Context) {
    private val signInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .requestEmail()
            .requestIdToken("YOUR_WEB_CLIENT_ID") // OAuth 客户端 ID
            .build()
    )

    fun signIn(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        launcher.launch(signInClient.signInIntent)
    }

    fun handleSignInResult(data: Intent?): GoogleAccountCredential? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = task.result
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        return credential
    }
}
