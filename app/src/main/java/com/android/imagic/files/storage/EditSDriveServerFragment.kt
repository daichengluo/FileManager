package com.android.imagic.files.storage

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.android.imagic.files.databinding.EditSdriveServerFragmentBinding
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.args
import com.android.imagic.files.util.finish
import com.android.imagic.files.util.setResult
import com.android.imagic.files.util.viewModels
import kotlinx.parcelize.Parcelize

class EditSDriveServerFragment : Fragment() {
    private val args by args<Args>()

    private val viewModel by viewModels { { EditSDriveServerViewModel() } }

    private lateinit var binding: EditSdriveServerFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = EditSdriveServerFragmentBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (args.server != null) {
            remove()
        }
        setupWebView()
        viewModel.wxQrcodeUrl.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {
                binding.webView.loadUrl(it)
            }
        }
        viewModel.loginSuccess.observe(viewLifecycleOwner) {
            if (it) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
        viewModel.getLoginUrl()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun handleOnQRCodeLoaded(qrCodeUrl: String) {
            }

            @JavascriptInterface
            fun handleOnScanSuccess() {
            }

            @JavascriptInterface
            fun handleOnCancelLogin() {
            }
        }, "WxQRCode")

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.url?.let {
                    val code = it.getQueryParameter("code") ?: ""
                    val state = it.getQueryParameter("state") ?: ""
                    viewModel.doLogin(code, state)
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(WX_JS_SCRIPT, null)
            }
        }
    }

    private fun remove() {
        Storages.remove(args.server!!)
        setResult(Activity.RESULT_OK)
        finish()
    }

    @Parcelize
    class Args(
        val server: SDriveServer? = null
    ) : ParcelableArgs

    companion object {
        val WX_JS_SCRIPT = """
           const imgWrapper = document.querySelector('.web_qrcode_img_wrp');
           const img = imgWrapper.querySelector('img');
           WxQRCode.handleOnQRCodeLoaded(img.src);
        
           const userScanned = document.querySelector('.js_wx_after_scan');
           const userCancelled = document.querySelector('.js_wx_after_cancel');
        
           const config = { attributes: true, attributeFilter: ['style'] };
        
           const callback = function(mutationsList, observer, element, androidCallback) {
               for (let mutation of mutationsList) {
                   if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
                       let style = window.getComputedStyle(element);
                       if (style.display === 'block') {
                           androidCallback();
                           observer.disconnect();
                       }
                   }
               }
           };
        
           const scanObserver = new MutationObserver(function(mutationsList, observer) {
               callback(mutationsList, observer, userScanned, function() {
                   WxQRCode.handleOnScanSuccess();
               });
           });
           scanObserver.observe(userScanned, config);
        
           const cancelObserver = new MutationObserver(function(mutationsList, observer) {
               callback(mutationsList, observer, userCancelled, function() {
                   WxQRCode.handleOnCancelLogin();
               });
           });
           cancelObserver.observe(userCancelled, config);
        """.trimIndent()
    }
}
