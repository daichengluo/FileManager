package com.android.imagic.files.cloud.util

import android.webkit.JavascriptInterface

class WechatWebInterface(
    private val onQRCodeLoaded: (String) -> Unit,
    private val onScanSuccess: () -> Unit,
    private val onCancelLogin: () -> Unit
) {
    @JavascriptInterface
    fun handleOnQRCodeLoaded(qrCodeUrl: String) {
        onQRCodeLoaded(qrCodeUrl)
    }

    @JavascriptInterface
    fun handleOnScanSuccess() {
        onScanSuccess()
    }

    @JavascriptInterface
    fun handleOnCancelLogin() {
        onCancelLogin()
    }
}