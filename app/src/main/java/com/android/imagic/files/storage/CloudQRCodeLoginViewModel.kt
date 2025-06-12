package com.android.imagic.files.storage

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.imagic.files.app.okHttpClient
import com.android.imagic.files.appwidget.AccessTokenData
import com.android.imagic.files.appwidget.AuthClientImpl
import com.android.imagic.files.appwidget.CloudClient
import com.android.imagic.files.appwidget.QrcodeLoginStatus
import com.android.imagic.files.appwidget.toAuthInfoBody
import com.android.imagic.files.appwidget.toAuthority
import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.cloud.sdrive.client.SDriveClient.OkHttpAuthenticatorInterceptor
import com.android.imagic.files.util.ApiOperation
import com.android.imagic.files.util.Constants
import com.android.imagic.files.util.NetworkUtil
import com.android.imagic.files.util.QrcodeUtil
import com.android.imagic.files.util.valueCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.FileOutputStream
import java.time.Clock

class CloudQRCodeLoginViewModel : ViewModel() {

    private val _qrcodeBitmap = MutableLiveData<Bitmap?>(null)
    val qrcodeBitmap: LiveData<Bitmap?> = _qrcodeBitmap

    private val _qrcodeLoginStatus = MutableLiveData<QrcodeLoginStatus>(QrcodeLoginStatus.UNSTARTED)
    val qrcodeLoginStatus: LiveData<QrcodeLoginStatus> = _qrcodeLoginStatus

    private val _accessTokenData = MutableLiveData<AccessTokenData?>(null)
    val accessTokenData: LiveData<AccessTokenData?> = _accessTokenData

    private val _authority = MutableLiveData<Authority?>(null)
    val authority: LiveData<Authority?> = _authority

    private var state: String? = null
    private var getStateJob: Job? = null
    private var getAccessTokenJob: Job? = null

    fun generateQrcode(context: Context) = viewModelScope.launch {
        val deviceId = NetworkUtil.getMacAddress() ?: getAndroidId(context)
        state = generateRandomString()
        val timestamp = Clock.systemUTC().instant().epochSecond.toString()
        val content =
            "https://wboard.starboard-online.com/Long.html?Mac=$deviceId&state=$state&timeStamp=$timestamp"
        Log.d("chengluo", "generateQrcode: $content")
        val bitmap = QrcodeUtil.generateQRCodeBitmap(content)
        _qrcodeBitmap.value = bitmap
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidId(context: Context) = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    fun getState() {
        if (getStateJob?.isActive == true) return
        if (state == null) return
        getStateJob = viewModelScope.launch {
            while (isActive && _qrcodeBitmap.valueCompat != null) {
                AuthClientImpl.getState(state!!)
                    .onSuccess {
                        Log.d("chengluo", "getState Success: $it")
                        _qrcodeLoginStatus.value = it
                        if (QrcodeLoginStatus.LOGGING == it) {
                            getStateJob?.cancel()
                            getStateJob = null
                        }
                    }
                    .onFailure {
                        Log.d("chengluo", "getState Failure: ${it.message}")
                        _qrcodeLoginStatus.value = QrcodeLoginStatus.FAILED
                    }
                delay(1000L)
            }
        }
    }

    fun getAccessToken() {
        if (getAccessTokenJob?.isActive == true) return
        if (state == null) return
        getAccessTokenJob = viewModelScope.launch {
            while (isActive && _qrcodeLoginStatus.valueCompat == QrcodeLoginStatus.LOGGING) {
                AuthClientImpl.getAccessToken(state!!)
                    .onSuccess {
                        Log.d("chengluo", "getAccessToken Success: $it")
                        _accessTokenData.value = it
                        if (it != null) {
                            getAccessTokenJob?.cancel()
                            getAccessTokenJob = null
                        }
                    }
                    .onFailure {
                        Log.d("chengluo", "getAccessToken Failure: ${it.message}")
                        getAccessTokenJob?.cancel()
                        getAccessTokenJob = null
                    }
                delay(1000L)
            }
        }
    }

    fun updateUserInfo(data: AccessTokenData) = viewModelScope.launch {
        Log.d("chengluo", "updateUserInfo: ")
        val headerImageFile = getUserHeaderImage(data.access_token)

        val authority = data.toAuthority()
        val authenticatorInterceptor = OkHttpAuthenticatorInterceptor(authority)
        val okHttpClient = okHttpClient.newBuilder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
            )
            .addNetworkInterceptor(authenticatorInterceptor)
            .authenticator(authenticatorInterceptor)
            .build()
        val client = CloudClient(okHttpClient)

        val headerImagerUrl = if (headerImageFile != null) {
            when (val result = client.uploadFileService(headerImageFile)) {
                is ApiOperation.Success -> result.data
                is ApiOperation.Failure -> null
            }
        } else null
        Log.d("chengluo", "updateUserInfo headerImagerUrl: $headerImagerUrl")

        client.getUserInfo(data.toAuthInfoBody(headerImagerUrl)).onSuccess {
            Log.d("chengluo", "updateUserInfo Success: $it")
            val newAuthority = getSpaceUsageInfo(it.toAuthority())
            Storages.addOrReplace(SDriveServer(null, null, newAuthority))

            if (it.isGoogle) {
                val gDriveServer = GDriveServer(null, null, it.access_token)
                Storages.addOrReplace(gDriveServer)
            }
            if (it.isMicrosoft) {
                val oneDriveServer = OneDriveServer(null, null, it.access_token)
                Storages.addOrReplace(oneDriveServer)
            }

            _authority.value = newAuthority
        }
    }

    private suspend fun getSpaceUsageInfo(authority: Authority): Authority {
        Log.d("chengluo", "getSpaceUsageInfo ")
        val authenticatorInterceptor = OkHttpAuthenticatorInterceptor(authority)
        val okHttpClient = okHttpClient.newBuilder()
            .addNetworkInterceptor(authenticatorInterceptor)
            .authenticator(authenticatorInterceptor)
            .build()
        val client = CloudClient(okHttpClient)
        return when (val response = client.getSpaceUsageInfo()) {
            is ApiOperation.Success -> {
                val data = response.data
                Log.d("chengluo", "getSpaceUsageInfo Success: $data")
                authority.copy(
                    freeSpace = data.remainingSpace,
                    totalSpace = data.totalSpace
                )
            }

            is ApiOperation.Failure -> authority
        }
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
            }
            return@withContext null
        }

    fun cancelJob() {
        getStateJob?.cancel()
        getAccessTokenJob?.cancel()
        getStateJob = null
        getAccessTokenJob = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelJob()
    }

    private fun generateRandomString(length: Int = 12): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
