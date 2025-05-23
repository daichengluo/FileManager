package com.android.imagic.files.storage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.databinding.CloudPasswordLoginFragmentBinding
import com.android.imagic.files.util.showToast
import com.android.imagic.files.util.viewModels

class CloudPasswordLoginFragment : Fragment() {

    private val sharedViewModel: EditSDriveServerViewModel by viewModels({ requireParentFragment() })

    private lateinit var binding: CloudPasswordLoginFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = CloudPasswordLoginFragmentBinding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginWithGoogle.setOnClickListener {
            if (!sharedViewModel.userAgreementChecked) {
                showToast(R.string.check_agreement_first)
                return@setOnClickListener
            }
        }
        binding.loginWithMicrosoft.setOnClickListener {
            if (!sharedViewModel.userAgreementChecked) {
                showToast(R.string.check_agreement_first)
                return@setOnClickListener
            }
        }
        binding.loginWithWechat.setOnClickListener {
            if (!sharedViewModel.userAgreementChecked) {
                showToast(R.string.check_agreement_first)
                return@setOnClickListener
            }
        }
    }

    companion object {
        const val TAG = "password"
    }
}
