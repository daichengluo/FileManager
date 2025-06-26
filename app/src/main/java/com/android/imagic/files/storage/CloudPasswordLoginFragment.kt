package com.android.imagic.files.storage

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.cloud.gdrive.sdk.GoogleDriveServiceClient
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.databinding.CloudPasswordLoginFragmentBinding
import com.android.imagic.files.util.finish
import com.android.imagic.files.util.showToast
import com.android.imagic.files.util.viewModels
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.CurrentAccountCallback
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CloudPasswordLoginFragment : Fragment() {

    private val sharedViewModel: EditSDriveServerViewModel by viewModels({ requireParentFragment() })

    private lateinit var binding: CloudPasswordLoginFragmentBinding

    private var msalApp: ISingleAccountPublicClientApplication? = null

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
            loginWithGoogle()
        }
        binding.loginWithMicrosoft.setOnClickListener {
            if (!sharedViewModel.userAgreementChecked) {
                showToast(R.string.check_agreement_first)
                return@setOnClickListener
            }
            PublicClientApplication.createSingleAccountPublicClientApplication(
                requireContext(),
                R.raw.msal_config,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        msalApp = application
                        loadMicrosoftAccountOrLogin()
                    }

                    override fun onError(exception: MsalException) {
                        Log.e("chengluo", "初始化失败: ${exception.message}")
                    }
                }
            )
        }
        binding.loginWithWechat.setOnClickListener {
            if (!sharedViewModel.userAgreementChecked) {
                showToast(R.string.check_agreement_first)
                return@setOnClickListener
            }
        }
    }

    private fun loadMicrosoftAccountOrLogin() {
        val currentAccountCallback = object : CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                Log.d("chengluo", "onAccountLoaded $activeAccount")
                if (activeAccount == null) {
                    microsoftLogin()
                } else {
                    acquireMicrosoftTokenSilentAsync(activeAccount)
                }
            }

            override fun onAccountChanged(
                priorAccount: IAccount?,
                currentAccount: IAccount?
            ) {
                if (currentAccount == null) {
                    Log.d("chengluo", "onAccountChanged OnSignOut")
                    microsoftLogin()
                }
            }

            override fun onError(exception: MsalException) {
                Log.e("chengluo", "createSingleAccountPublicClientApplication error: $exception")
            }
        }
        msalApp?.getCurrentAccountAsync(currentAccountCallback)
    }

    private fun microsoftLogin() {
        val authInteractiveCallback = object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                Log.d("chengluo", "Successfully authenticated")
                val accessToken = authenticationResult.accessToken
                val claims = authenticationResult.account.claims
                val email = claims?.get("preferred_username") as String?
                val locale = claims?.get("locale") as String?
                Log.d("chengluo", "accessToken $accessToken email: $email locale: $locale")
            }

            override fun onError(exception: MsalException) {
                Log.d("chengluo", "Authentication failed: $exception")
            }

            override fun onCancel() {
                Log.d("chengluo", "User cancelled login.")
            }
        }

        val signInParameters = SignInParameters.builder()
            .withActivity(requireActivity())
            .withLoginHint(null)
            .withScopes(microsoftScopes)
            .withCallback(authInteractiveCallback)
            .build()
        msalApp?.signIn(signInParameters)
    }

    private fun acquireMicrosoftTokenSilentAsync(account: IAccount) {
        val silentAuthenticationCallback = object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                Log.d("chengluo", "Successfully authenticated")
                val claims = authenticationResult.account.claims
                val email = claims?.get("preferred_username") as String?
                val accessToken = authenticationResult.accessToken
                Log.d("chengluo", "accessToken $accessToken email: $email")
            }

            override fun onError(exception: MsalException) {
                Log.d("chengluo", "Authentication failed: $exception")
            }
        }

        val silentParameters = AcquireTokenSilentParameters.Builder()
            .fromAuthority(account.authority)
            .forAccount(account)
            .withScopes(microsoftScopes)
            .withCallback(silentAuthenticationCallback)
            .build()
        msalApp?.acquireTokenSilentAsync(silentParameters)
    }

    private fun loginWithGoogle() {
        Log.d("chengluo", "loginWithGoogle: ")
        val serverClientId = getString(R.string.web_client_id)
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestIdToken(serverClientId)
            .requestProfile()
            .requestId()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), signInOptions)
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                task.result?.let { account ->
                    val authority = ServerAuthority(
                        openId = account.id ?: "",
                        userId = 0,
                        username = account.displayName ?: "",
                        email = account.email ?: "",
                        avatarUrl = account.photoUrl?.toString(),
                        accessToken = "",
                        refreshToken = "",
                        expireAt = 0,
                        type = 1,
                        totalSpace = 0L,
                        usedSpace = 0L
                    )
                    val storage =
                        GoogleDriveServer(id = null, customName = null, authority = authority)
                    Storages.addOrReplace(storage)
                    GoogleDriveServiceClient.getService(authority, account, requireContext())
                    finish()
                }
            } else {
                Log.e("chengluo", "登录失败：${task.exception}")
            }
        }
    }

    private suspend fun getGoogleAccessToken(account: GoogleSignInAccount): String =
        withContext(Dispatchers.IO) {
            val scope = "oauth2:${DriveScopes.DRIVE_FILE}"
            try {
                GoogleAuthUtil.getToken(requireContext(), account.account!!, scope)
            } catch (e: Exception) {
                throw RuntimeException("获取 token 失败: ${e.message}", e)
            }
        }

    private fun ensureGooglePlayServices(): Boolean {
        val activity = requireActivity()
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(activity)

        return if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.showErrorDialogFragment(activity, status, REQUEST_CODE)
            } else {
                Log.e(
                    "chengluo",
                    "GooglePlayService: Google Play Services not resolvable: status = $status"
                )
            }
            false
        } else {
            Log.d("chengluo", "GooglePlayService: Google Play Services is available")
            true
        }
    }

    companion object {
        const val TAG = "password"

        const val REQUEST_CODE = 7007
        const val RC_SIGN_IN = 6007

        val microsoftScopes = listOf("User.Read", "openid", "profile", "email", "Files.ReadWrite")
    }
}
