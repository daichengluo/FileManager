package com.android.imagic.files.cloud

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.android.imagic.files.R
import com.android.imagic.files.databinding.CloudLoginFragmentBinding
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.args
import com.android.imagic.files.util.showToast
import com.android.imagic.files.util.viewModels
import kotlinx.parcelize.Parcelize

class CloudLoginFragment : Fragment() {
    private val args by args<Args>()

    private val viewModel by viewModels { { CloudLoginViewModel() } }

    private lateinit var binding: CloudLoginFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = CloudLoginFragmentBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fm = childFragmentManager

        if (fm.findFragmentById(R.id.fragmentContainer) == null) {
            val password = fm.findFragmentByTag(CloudLoginMethod.Password.toString())
                ?: PasswordLoginFragment()
            val qrcode = fm.findFragmentByTag(CloudLoginMethod.QRCode.toString())
                ?: QRCodeLoginFragment()

            fm.commit {
                add(R.id.fragmentContainer, password, CloudLoginMethod.Password.toString())
                add(R.id.fragmentContainer, qrcode, CloudLoginMethod.QRCode.toString())
                hide(qrcode)
                show(password)
            }
        }

        binding.fragmentContainer.setBackgroundResource(
            if (args.fromWidget) {
                R.drawable.bg_qrcode_login_fragment
            } else {
                R.drawable.bg_navigation_login
            }
        )

        viewModel.userAgreementCheckedLiveData.observe(viewLifecycleOwner) { checked ->
            binding.userAgreementButton.setImageResource(
                if (checked) R.drawable.useragreement_checked
                else R.drawable.useragreement_unchecked_grey
            )
        }

        viewModel.currentLoginMethodLiveData.observe(viewLifecycleOwner) { currentMethod ->

            val password = fm.findFragmentByTag(CloudLoginMethod.Password.toString())
            val qrcode = fm.findFragmentByTag(CloudLoginMethod.QRCode.toString())

            when (currentMethod) {
                CloudLoginMethod.QRCode -> {
                    fm.commit {
                        password?.let { hide(it) }
                        qrcode?.let { show(it) }
                    }

                    binding.userAgreementLayout.isVisible = false
                    binding.loginSwitchButton.setImageResource(R.drawable.password_login_method)
                    binding.loginSwitchButton.contentDescription =
                        getString(R.string.use_password_login)
                }

                CloudLoginMethod.Password -> {
                    fm.commit {
                        qrcode?.let { hide(it) }
                        password?.let { show(it) }
                    }

                    binding.userAgreementLayout.isVisible = true
                    binding.loginSwitchButton.setImageResource(R.drawable.qrcode_login_method)
                    binding.loginSwitchButton.contentDescription =
                        getString(R.string.use_qrcode_login)
                }
            }
        }

        binding.loginSwitchButton.setOnClickListener {
            if (viewModel.currentLoginMethod == CloudLoginMethod.Password
                && !viewModel.userAgreementChecked
            ) {
                showToast(R.string.check_agreement_first)
                return@setOnClickListener
            }
            viewModel.switchLoginMethod()
        }

        binding.userAgreementButton.setOnClickListener {
            viewModel.updateUserAgreementChecked()
        }

        val userAgreement = getString(R.string.user_agreement)
        val privacyPolicy = getString(R.string.privacy_policy)

        val fullText = getString(R.string.user_agreement_text, userAgreement, privacyPolicy)

        val spannable = SpannableString(fullText)

        val userAgreementStart = fullText.indexOf(userAgreement)
        val privacyPolicyStart = fullText.indexOf(privacyPolicy)

        if (userAgreementStart != -1) {
            spannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://www.starboard-online.com/static/Agreement.html".toUri()
                        )
                        widget.context.startActivity(intent)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.isUnderlineText = true
                    }
                },
                userAgreementStart,
                userAgreementStart + userAgreement.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (privacyPolicyStart != -1) {
            spannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://www.starboard-online.com/static/Agreement.html".toUri()
                        )
                        widget.context.startActivity(intent)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.isUnderlineText = true
                    }
                },
                privacyPolicyStart,
                privacyPolicyStart + privacyPolicy.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.userAgreementText.text = spannable
        binding.userAgreementText.movementMethod = LinkMovementMethod.getInstance()
        binding.userAgreementText.highlightColor = Color.TRANSPARENT
    }

    @Parcelize
    class Args(
        val fromWidget: Boolean = false,
        val qrcodeLogin: Boolean = true
    ) : ParcelableArgs
}
