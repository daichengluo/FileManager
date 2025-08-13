package com.android.imagic.files.storage

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.imagic.files.R
import com.android.imagic.files.app.okHttpClient
import com.android.imagic.files.appwidget.CloudClient
import com.android.imagic.files.appwidget.LoginWidgetUpdater
import com.android.imagic.files.appwidget.toAuthority
import com.android.imagic.files.cloud.gdrive.sdk.GoogleDriveServiceClient
import com.android.imagic.files.cloud.onedrive.MsalApp
import com.android.imagic.files.cloud.onedrive.OneDriveClient
import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.cloud.sdrive.client.StarBoardDriveClient.OkHttpAuthenticatorInterceptor
import com.android.imagic.files.cloud.util.ServerAuthority
import com.android.imagic.files.cloud.util.toAuthInfoBody
import com.android.imagic.files.cloud.util.toStarBoardAuthority
import com.android.imagic.files.databinding.CloudPasswordLoginFragmentBinding
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.Constants
import com.android.imagic.files.util.finish
import com.android.imagic.files.util.showToast
import com.android.imagic.files.util.valueCompat
import com.android.imagic.files.util.viewModels
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.CurrentAccountCallback
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class CloudPasswordLoginFragment : Fragment() {

    private val sharedViewModel: EditStarBoardDriveServerViewModel by viewModels({ requireParentFragment() })

    private lateinit var binding: CloudPasswordLoginFragmentBinding

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
            MsalApp.initMsalApp(requireContext(), ::loadMicrosoftAccountOrLogin)
        }
        binding.loginWithWechat.setOnClickListener {
            if (!sharedViewModel.userAgreementChecked) {
                showToast(R.string.check_agreement_first)
                return@setOnClickListener
            }
        }
    }

    private fun loadMicrosoftAccountOrLogin(msalApp: ISingleAccountPublicClientApplication) {
        val currentAccountCallback = object : CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                Log.d("chengluo", "onAccountLoaded $activeAccount")
                if (activeAccount == null) {
                    microsoftLogin(msalApp)
                } else {
                    acquireMicrosoftTokenSilentAsync(msalApp, activeAccount)
                }
            }

            override fun onAccountChanged(
                priorAccount: IAccount?,
                currentAccount: IAccount?
            ) {
                if (currentAccount == null) {
                    Log.d("chengluo", "onAccountChanged OnSignOut")
                    microsoftLogin(msalApp)
                }
            }

            override fun onError(exception: MsalException) {
                Log.e("chengluo", "createSingleAccountPublicClientApplication error: $exception")
                showToast(R.string.login_failed_retry)
            }
        }
        msalApp.getCurrentAccountAsync(currentAccountCallback)
    }

    private fun microsoftLogin(msalApp: ISingleAccountPublicClientApplication) {
        val authInteractiveCallback = object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                val claims = authenticationResult.account.claims
                val accessToken = authenticationResult.accessToken
                val expireAt = authenticationResult.expiresOn.time

                val authority = ServerAuthority(
                    openId = claims?.get("oid") as? String ?: "",
                    username = claims?.get("name") as? String ?: "",
                    email = claims?.get("preferred_username") as? String ?: "",
                    avatarUrl = null,
                    accessToken = accessToken,
                    refreshToken = "",
                    expireAt = expireAt,
                    type = 2,
                    totalSpace = 0L,
                    usedSpace = 0L
                )
                lifecycleScope.launch {
                    binding.progressBarLayout.isVisible = true
                    val response = OneDriveClient.getSpaceData(authority)
                    var updatedAuthority = if (response is ApiOperation.Success) {
                        val data = response.data
                        authority.copy(
                            totalSpace = data.total,
                            usedSpace = data.used
                        )
                    } else authority

                    val starBoardAuthority = updatedAuthority.toStarBoardAuthority()
                    val authenticatorInterceptor =
                        OkHttpAuthenticatorInterceptor(starBoardAuthority)
                    val okHttpClient = okHttpClient.newBuilder()
                        .addInterceptor(
                            HttpLoggingInterceptor().apply {
                                level = HttpLoggingInterceptor.Level.BODY
                            }
                        )
                        .addNetworkInterceptor(authenticatorInterceptor)
                        .authenticator(authenticatorInterceptor)
                        .build()
                    val starBoardClient = CloudClient(okHttpClient)
                    CloudClient(okHttpClient).getUserInfo(updatedAuthority.toAuthInfoBody())
                        .onSuccess { loginInfo ->
                            val starBoardServer =
                                StarBoardDriveServer(null, null, loginInfo.toAuthority())
                            Settings.STORAGES.valueCompat.find { it is StarBoardDriveServer }
                                ?.let { Storages.remove(it) }
                            Storages.addOrReplace(starBoardServer)
                        }
                        .onFailure { e ->
                            e.printStackTrace()
                        }
                    val headImageUrl = getUserHeaderImage(updatedAuthority.accessToken)?.let {
                        val response = starBoardClient.uploadFileService(it)
                        if (response is ApiOperation.Success) {
                            response.data
                        } else null
                    }
                    updatedAuthority = updatedAuthority.copy(
                        avatarUrl = headImageUrl
                    )
                    val storage = OneDriveServer(
                        id = null,
                        customName = null,
                        authority = updatedAuthority
                    )
                    Settings.STORAGES.valueCompat.find { it is OneDriveServer }
                        ?.let { Storages.remove(it) }
                    Storages.addOrReplace(storage)
                    LoginWidgetUpdater.updateWidgets(requireContext())
                    finish()
                }
            }

            override fun onError(exception: MsalException) {
                Log.d("chengluo", "Authentication failed: $exception")
                showToast(R.string.login_failed_retry)
            }

            override fun onCancel() {
                Log.d("chengluo", "User cancelled login.")
                showToast(R.string.login_user_cancelled)
            }
        }

        val signInParameters = SignInParameters.builder()
            .withActivity(requireActivity())
            .withLoginHint(null)
            .withScopes(microsoftScopes)
            .withCallback(authInteractiveCallback)
            .build()
        msalApp.signIn(signInParameters)
    }

    private suspend fun getUserHeaderImage(accessToken: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(Constants.MS_GRAPH_PHOTO)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    return@withContext null
                }

                val bytes = response.body?.bytes()
                response.close()

                if (bytes != null) {
                    val file = File.createTempFile("userHeaderImage", ".jpg")
                    FileOutputStream(file).use { it.write(bytes) }
                    return@withContext file
                }
            } catch (e: Exception) {
                Log.e("chengluo", "getUserHeaderImage: ", e)
                showToast(R.string.load_user_image_failed)
            }
            return@withContext null
        }

    private fun acquireMicrosoftTokenSilentAsync(
        msalApp: ISingleAccountPublicClientApplication,
        account: IAccount
    ) {
        val silentAuthenticationCallback = object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                val claims = authenticationResult.account.claims
                val accessToken = authenticationResult.accessToken
                val expireAt = authenticationResult.expiresOn.time

                val authority = ServerAuthority(
                    openId = claims?.get("oid") as? String ?: "",
                    username = claims?.get("name") as? String ?: "",
                    email = claims?.get("preferred_username") as? String ?: "",
                    avatarUrl = null,
                    accessToken = accessToken,
                    refreshToken = "",
                    expireAt = expireAt,
                    type = 2,
                    totalSpace = 0L,
                    usedSpace = 0L
                )
                lifecycleScope.launch {
                    binding.progressBarLayout.isVisible = true
                    val response = OneDriveClient.getSpaceData(authority)
                    val updatedAuthority = if (response is ApiOperation.Success) {
                        val data = response.data
                        authority.copy(
                            totalSpace = data.total,
                            usedSpace = data.used
                        )
                    } else authority
                    val storage = OneDriveServer(
                        id = null,
                        customName = null,
                        authority = updatedAuthority
                    )
                    Settings.STORAGES.valueCompat.find { it is OneDriveServer }
                        ?.let { Storages.remove(it) }
                    Storages.addOrReplace(storage)

                    val starBoardAuthority = updatedAuthority.toStarBoardAuthority()
                    val authenticatorInterceptor =
                        OkHttpAuthenticatorInterceptor(starBoardAuthority)
                    val okHttpClient = okHttpClient.newBuilder()
                        .addInterceptor(
                            HttpLoggingInterceptor().apply {
                                level = HttpLoggingInterceptor.Level.BODY
                            }
                        )
                        .addNetworkInterceptor(authenticatorInterceptor)
                        .authenticator(authenticatorInterceptor)
                        .build()
                    CloudClient(okHttpClient).getUserInfo(updatedAuthority.toAuthInfoBody())
                        .onSuccess { loginInfo ->
                            val starBoardServer =
                                StarBoardDriveServer(null, null, loginInfo.toAuthority())
                            Settings.STORAGES.valueCompat.find { it is StarBoardDriveServer }
                                ?.let { Storages.remove(it) }
                            Storages.addOrReplace(starBoardServer)
                            LoginWidgetUpdater.updateWidgets(requireContext())
                        }
                        .onFailure { e ->
                            e.printStackTrace()
                        }
                    finish()
                }
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
        msalApp.acquireTokenSilentAsync(silentParameters)
    }

    private fun loginWithGoogle() {
        Log.d("chengluo", "loginWithGoogle: ")
        val serverClientId = getString(R.string.web_client_id)
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestIdToken(serverClientId)
            .requestServerAuthCode(serverClientId)
            .requestProfile()
            .requestId()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), signInOptions)
        try {
            signInLauncher.launch(googleSignInClient.signInIntent)
        } catch (e: Exception) {
            Log.e("chengluo", "loginWithGoogle error: ", e)
            showToast(R.string.login_failed_retry)
        }
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("chengluo", "StartActivityForResult: ${result.data}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                task.result?.let { account ->
                    val serverAuthority = ServerAuthority(
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
                    lifecycleScope.launch {
                        GoogleDriveServiceClient.getService(
                            serverAuthority,
                            account,
                            requireContext()
                        )
                        binding.progressBarLayout.isVisible = true
                        val updatedAuthority =
                            GoogleDriveServiceClient.getSpaceData(serverAuthority)?.let {
                                serverAuthority.copy(totalSpace = it.total, usedSpace = it.used)
                            } ?: serverAuthority
                        val storage = GoogleDriveServer(
                            id = null,
                            customName = null,
                            authority = updatedAuthority
                        )
                        Settings.STORAGES.valueCompat.find { it is GoogleDriveServer }
                            ?.let { Storages.remove(it) }
                        Storages.addOrReplace(storage)
                    }

                    lifecycleScope.launch {
                        val authority = Authority(
                            openId = account.id ?: "",
                            userId = 0,
                            username = account.displayName ?: "",
                            headerImageUrl = account.photoUrl?.toString(),
                            accessToken = "",
                        )
                        val authenticatorInterceptor = OkHttpAuthenticatorInterceptor(authority)
                        val okHttpClient = OkHttpClient().newBuilder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .addInterceptor(
                                HttpLoggingInterceptor().apply {
                                    level = HttpLoggingInterceptor.Level.BODY
                                }
                            )
                            .addNetworkInterceptor(authenticatorInterceptor)
                            .authenticator(authenticatorInterceptor)
                            .build()
                        CloudClient(okHttpClient).getUserInfo(serverAuthority.toAuthInfoBody())
                            .onSuccess { loginInfo ->
                                val starBoardServer =
                                    StarBoardDriveServer(null, null, loginInfo.toAuthority())
                                Storages.addOrReplace(starBoardServer)
                                LoginWidgetUpdater.updateWidgets(requireContext())
                            }
                            .onFailure { e ->
                                Log.e("chengluo", "google starboard doLogin error: ", e)
                            }
                        finish()
                    }
                }
            } else {
                showToast(getString(R.string.login_failed_message, task.exception?.message ?: ""))
            }
        } else {
            showToast(R.string.login_failed_retry)
        }
    }

    companion object {
        const val TAG = "password"

        val microsoftScopes = listOf("User.Read", "openid", "profile", "email", "Files.ReadWrite")

        fun generateNonceStr(length: Int): String {
            val allowedChars = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }
    }
}
