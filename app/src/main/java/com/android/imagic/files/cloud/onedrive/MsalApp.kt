package com.android.imagic.files.cloud.onedrive

import android.content.Context
import android.util.Log
import com.android.imagic.files.R
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException

object MsalApp {

    private var msalApp: ISingleAccountPublicClientApplication? = null

    fun initMsalApp(
        context: Context,
        onSuccess: ((msalApp: ISingleAccountPublicClientApplication) -> Unit)? = null,
        onError: ((e: MsalException) -> Unit)? = null
    ) {
        Log.d("chengluo", "initMsalApp: ")
        if (msalApp == null) {
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.msal_config,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        msalApp = application
                        onSuccess?.invoke(msalApp!!)
                        Log.d("chengluo", "initMsalApp onCreated: ")
                    }

                    override fun onError(exception: MsalException) {
                        Log.e(
                            "chengluo",
                            "initMsalApp error: ${exception.message}"
                        )
                        onError?.invoke(exception)
                    }
                }
            )
        } else {
            onSuccess?.invoke(msalApp!!)
        }
    }
}