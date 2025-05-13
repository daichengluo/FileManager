package com.android.imagic.files.appwidget

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.imagic.files.util.NetworkUtil
import com.android.imagic.files.util.QrcodeUtil
import com.android.imagic.files.util.valueCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock

class QrCodeLoginViewModel : ViewModel() {

    private val _qrcodeBitmap = MutableLiveData<Bitmap?>(null)
    val qrcodeBitmap: LiveData<Bitmap?> = _qrcodeBitmap

    private val _qrcodeLoginStatus = MutableLiveData<QrcodeLoginStatus>(QrcodeLoginStatus.UNSTARTED)
    val qrcodeLoginStatus: LiveData<QrcodeLoginStatus> = _qrcodeLoginStatus

    private val _accessTokenData = MutableLiveData<AccessTokenData?>(null)
    val accessTokenData: LiveData<AccessTokenData?> = _accessTokenData

    private var state: String? = null
    private var getStateJob: Job? = null
    private var getAccessTokenJob: Job? = null

    fun generateQrcode() = viewModelScope.launch {
        NetworkUtil.getMacAddress()?.let { macAddress ->
            state = generateRandomString()
            val timestamp = Clock.systemUTC().instant().epochSecond.toString()
            val content =
                "https://wboard.starboard-online.com/Long.html?Mac=$macAddress&state=$state&timeStamp=$timestamp"
            Log.d("chengluo", "generateQrcode: $content")
            val bitmap = QrcodeUtil.generateQRCodeBitmap(content)
            _qrcodeBitmap.value = bitmap
        }
    }

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
