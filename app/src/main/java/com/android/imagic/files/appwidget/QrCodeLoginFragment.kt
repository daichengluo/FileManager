package com.android.imagic.files.appwidget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.android.imagic.files.databinding.QrcodeLoginBinding
import com.android.imagic.files.util.viewModels

class QrCodeLoginFragment : Fragment() {

    private val viewModel by viewModels { { QrCodeLoginViewModel() } }

    private lateinit var binding: QrcodeLoginBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = QrcodeLoginBinding.inflate(inflater, container, false)
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

        viewModel.generateQrcode()
    }
}
