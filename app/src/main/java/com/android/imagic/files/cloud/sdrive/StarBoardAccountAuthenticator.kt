package com.android.imagic.files.cloud.sdrive

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.os.Binder
import android.os.Bundle
import com.android.imagic.files.storage.EditStarBoardDriveServerActivity
import com.android.imagic.files.util.createIntent

class StarBoardAccountAuthenticator(private val context: Context) :
    AbstractAccountAuthenticator(context) {

    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String?,
        requiredFeatures: Array<String>?,
        options: Bundle?
    ): Bundle {
        val intent = EditStarBoardDriveServerActivity::class.createIntent().apply {
            putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        }
        return Bundle().apply {
            putParcelable(AccountManager.KEY_INTENT, intent)
        }
    }

    override fun confirmCredentials(
        p0: AccountAuthenticatorResponse?,
        p1: Account?,
        p2: Bundle?
    ): Bundle? = throw UnsupportedOperationException()

    override fun editProperties(
        p0: AccountAuthenticatorResponse?,
        p1: String?
    ): Bundle? = throw UnsupportedOperationException()

    override fun getAuthToken(
        p0: AccountAuthenticatorResponse?,
        p1: Account?,
        p2: String?,
        p3: Bundle?
    ): Bundle? {
        val callingUid = Binder.getCallingUid()
        val pm = context.packageManager
        val callingPackages = pm.getPackagesForUid(callingUid) ?: emptyArray()

        if (callingPackages.any { it == "com.trusted.app" }) {
            val token = "your-access-token-from-storage"
            return Bundle().apply {
                putString(AccountManager.KEY_ACCOUNT_NAME, "username")
                putString(AccountManager.KEY_ACCOUNT_TYPE, "type")
                putString(AccountManager.KEY_AUTHTOKEN, token)
            }
        } else {
            throw SecurityException("Unauthorized access to token")
        }
    }

    override fun getAuthTokenLabel(p0: String?): String? = throw UnsupportedOperationException()

    override fun hasFeatures(
        p0: AccountAuthenticatorResponse?,
        p1: Account?,
        p2: Array<out String?>?
    ): Bundle? = throw UnsupportedOperationException()

    override fun updateCredentials(
        p0: AccountAuthenticatorResponse?,
        p1: Account?,
        p2: String?,
        p3: Bundle?
    ): Bundle? = throw UnsupportedOperationException()
}