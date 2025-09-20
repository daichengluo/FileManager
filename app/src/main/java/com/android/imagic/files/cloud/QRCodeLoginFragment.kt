package com.android.imagic.files.cloud

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.android.imagic.files.appwidget.LoginWidgetUpdater
import com.android.imagic.files.appwidget.QrcodeLoginStatus
import com.android.imagic.files.databinding.QrcodeLoginFragmentBinding
import com.android.imagic.files.util.finish
import com.android.imagic.files.util.viewModels

class QRCodeLoginFragment : Fragment() {

    private val viewModel by viewModels { { QRCodeLoginViewModel() } }

    private lateinit var binding: QrcodeLoginFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = QrcodeLoginFragmentBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.qrcodeBitmap.observe(viewLifecycleOwner) { qrcodeBitmap ->
            if (qrcodeBitmap != null) {
                binding.qrcodeImageView.setImageBitmap(qrcodeBitmap)
                viewModel.getState()
            }
            binding.progressBar.isVisible = qrcodeBitmap == null
        }

        viewModel.qrcodeLoginStatus.observe(viewLifecycleOwner) { qrcodeLoginStatus ->
            if (qrcodeLoginStatus == QrcodeLoginStatus.LOGGING) {
                viewModel.getAccessToken()
            }
        }

        viewModel.accessTokenData.observe(viewLifecycleOwner) { accessTokenData ->
            if (accessTokenData != null) {
                viewModel.updateUserInfo(accessTokenData)
            }
        }

        viewModel.authority.observe(viewLifecycleOwner) { authority ->
            if (authority != null) {
                LoginWidgetUpdater.updateWidgets(requireContext())

                val account = Account(authority.username, "com.android.imagic.account")
                val accountManager = AccountManager.get(context)

                val success = accountManager.addAccountExplicitly(
                    account,
                    authority.signTimestamp.first,
                    null
                )
                if (success) {
                    // 可以设置 authToken
                    accountManager.setAuthToken(account, "full_access", authority.accessToken)
                }
                finish()
            }
        }

        viewModel.isLoadingUserInfoLiveData.observe(viewLifecycleOwner) { loading ->
            binding.progressBarLayout.isVisible = loading
        }

        viewModel.generateQrcode(requireContext())
    }
}
