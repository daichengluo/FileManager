package com.android.imagic.files.storage

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
import com.android.imagic.files.databinding.EditSdriveServerFragmentBinding
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.args
import com.android.imagic.files.util.viewModels
import kotlinx.parcelize.Parcelize

class EditStarBoardDriveServerFragment : Fragment() {
    private val args by args<Args>()

    private val viewModel by viewModels { { EditStarBoardDriveServerViewModel() } }

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

        val fm = childFragmentManager
        val password = fm.findFragmentByTag(CloudPasswordLoginFragment.TAG)
            ?: CloudPasswordLoginFragment()
        val qrcode = fm.findFragmentByTag(CloudQRCodeLoginFragment.TAG)
            ?: CloudQRCodeLoginFragment()

        if (fm.findFragmentById(R.id.fragmentContainer) == null) {
            fm.commit {
                add(R.id.fragmentContainer, password, CloudPasswordLoginFragment.TAG)
                add(R.id.fragmentContainer, qrcode, CloudQRCodeLoginFragment.TAG)
                hide(password)
            }
            binding.loginSwitchButton.setImageResource(R.drawable.password_login_method)
            binding.loginSwitchButton.contentDescription = getString(R.string.use_password_login)
            binding.userAgreementLayout.isVisible = false
            binding.qrcodeUserAgreementText.isVisible = true
        }

        binding.loginSwitchButton.setOnClickListener {
            val currentPassword = fm.findFragmentByTag(CloudPasswordLoginFragment.TAG)
            val currentQRCode = fm.findFragmentByTag(CloudQRCodeLoginFragment.TAG)

            if (currentQRCode != null && currentQRCode.isVisible) {
                fm.commit {
                    hide(currentQRCode)
                    show(currentPassword!!)
                }
                binding.loginSwitchButton.setImageResource(R.drawable.qrcode_login_method)
                binding.loginSwitchButton.contentDescription = getString(R.string.use_qrcode_login)
                binding.qrcodeUserAgreementText.isVisible = false
                binding.userAgreementLayout.isVisible = true
            } else {
                fm.commit {
                    hide(currentPassword!!)
                    show(currentQRCode!!)
                }
                binding.loginSwitchButton.setImageResource(R.drawable.password_login_method)
                binding.loginSwitchButton.contentDescription =
                    getString(R.string.use_password_login)
                binding.userAgreementLayout.isVisible = false
                binding.qrcodeUserAgreementText.isVisible = true
            }
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

        val qrcodeLoginFullText =
            getString(R.string.qrcode_login_user_agreement_text, userAgreement, privacyPolicy)

        val qrcodeLoginSpannable = SpannableString(qrcodeLoginFullText)

        val qrcodeLoginUserAgreementStart = qrcodeLoginFullText.indexOf(userAgreement)
        val qrcodeLoginPrivacyPolicyStart = qrcodeLoginFullText.indexOf(privacyPolicy)

        if (qrcodeLoginUserAgreementStart != -1) {
            qrcodeLoginSpannable.setSpan(
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
                qrcodeLoginUserAgreementStart,
                qrcodeLoginUserAgreementStart + userAgreement.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (qrcodeLoginPrivacyPolicyStart != -1) {
            qrcodeLoginSpannable.setSpan(
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
                qrcodeLoginPrivacyPolicyStart,
                qrcodeLoginPrivacyPolicyStart + privacyPolicy.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.qrcodeUserAgreementText.text = qrcodeLoginSpannable
        binding.qrcodeUserAgreementText.movementMethod = LinkMovementMethod.getInstance()
        binding.qrcodeUserAgreementText.highlightColor = Color.TRANSPARENT
    }

    @Parcelize
    class Args(val fromWidget: Boolean = false) : ParcelableArgs
}
