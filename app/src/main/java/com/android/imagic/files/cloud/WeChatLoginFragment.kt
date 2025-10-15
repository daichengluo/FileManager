package com.android.imagic.files.cloud

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import coil.load
import com.android.imagic.files.R
import com.android.imagic.files.appwidget.LoginWidgetUpdater
import com.android.imagic.files.cloud.util.WechatWebInterface
import com.android.imagic.files.databinding.WechatLoginFragmentBinding
import com.android.imagic.files.storage.StarBoardDriveServer
import com.android.imagic.files.storage.Storages
import com.android.imagic.files.util.finish
import com.android.imagic.files.util.viewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeChatLoginFragment : Fragment() {

    private val viewModel by viewModels { { WeChatLoginViewModel() } }

    private lateinit var binding: WechatLoginFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = WechatLoginFragmentBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.wechatQRCodeUrlLiveData.observe(viewLifecycleOwner) { wechatQRCodeUrl ->
            if (wechatQRCodeUrl == null) {
                binding.webview.clearCache(true)
                viewModel.stopScheduledReload()
                binding.qrcodeImageView.isVisible = false
                binding.failureLayout.isVisible = false
                binding.scanSuccessLayout.isVisible = false
                binding.qrcodeLayout.isVisible = true
                binding.progressBar.isVisible = true
            } else {
                loadWeChatQRCode(wechatQRCodeUrl)
            }
        }

        viewModel.authorityLiveData.observe(viewLifecycleOwner) { authority ->
            if (authority != null) {
                val starBoardServer = StarBoardDriveServer(null, null, authority)
                Storages.addOrReplace(starBoardServer)
                LoginWidgetUpdater.updateWidgets(requireContext())
                finish()
            }
        }

        val back: () -> Unit = {
            (parentFragment as? CloudLoginFragment)?.showLoginFragment(CloudLoginMethod.Password)
            parentFragmentManager.commit { remove(this@WeChatLoginFragment) }
        }

        binding.backButton.setOnClickListener { back() }
        binding.failureBackButton.setOnClickListener { back() }
        binding.retryButton.setOnClickListener {
            viewModel.reLoadQRCode()
            binding.webview.reload()
        }

        viewModel.getLoginUrl()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadWeChatQRCode(qrcodeUrl: String) {
        binding.webview.apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(
                WechatWebInterface(
                    { qrcodeUrl ->
                        Log.d("chengluo", "onQRCodeUrlLoaded: $qrcodeUrl")
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.failureLayout.isVisible = false
                            binding.qrcodeImageView.apply {
                                isVisible = true
                                load(qrcodeUrl) {
                                    listener(
                                        onSuccess = { request, metadata ->
                                            Log.d("chengluo", "qrcode load success")
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                binding.progressBar.isVisible = false
                                                binding.qrcodeImageView.isVisible = true
                                            }
                                            viewModel.startScheduledReload()
                                        },
                                        onError = { request, throwable ->
                                            Log.d("chengluo", "qrcode load failure")
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                binding.qrcodeLayout.isVisible = false
                                                binding.scanSuccessLayout.isVisible = false
                                                binding.progressBar.isVisible = false
                                                binding.failureTextView.text =
                                                    getString(R.string.loading_failed)
                                                binding.failureLayout.isVisible = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    {
                        Log.d("chengluo", "onScanSuccess: ")
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.qrcodeLayout.isVisible = false
                            binding.failureLayout.isVisible = false
                            binding.scanSuccessLayout.isVisible = true
                        }
                    },
                    {
                        Log.d("chengluo", "onCancelLogin: ")
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.qrcodeLayout.isVisible = false
                            binding.scanSuccessLayout.isVisible = false
                            binding.failureTextView.text =
                                getString(R.string.login_has_been_canceled)
                            binding.failureLayout.isVisible = true
                        }
                    }
                ),
                "WeChatWebView"
            )

            webViewClient = object : WebViewClient() {

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url.toString().toUri()
                    val code = uri.getQueryParameter("code")
                    val state = uri.getQueryParameter("state")
                    Log.d("chengluo", "scan success, code: $code state: $state")
                    if (code != null && state != null) {
                        viewModel.doLogin(code, state)
                    }
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    view?.evaluateJavascript(
                        """
                            const imgWrapper = document.querySelector('.web_qrcode_img_wrp');
                            const img = imgWrapper.querySelector('img');
                            WeChatWebView.handleOnQRCodeLoaded(img.src);

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
                                    WeChatWebView.handleOnScanSuccess();
                                });
                            });
                            scanObserver.observe(userScanned, config);

                            const cancelObserver = new MutationObserver(function(mutationsList, observer) {
                                callback(mutationsList, observer, userCancelled, function() {
                                    WeChatWebView.handleOnCancelLogin();
                                });
                            });
                            cancelObserver.observe(userCancelled, config);
                        """.trimIndent(), null
                    )
                }
            }
        }
        binding.webview.loadUrl(qrcodeUrl)
    }
}
