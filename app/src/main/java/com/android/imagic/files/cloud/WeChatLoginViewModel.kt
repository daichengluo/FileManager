package com.android.imagic.files.cloud

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.imagic.files.app.okHttpClient
import com.android.imagic.files.appwidget.CloudClient
import com.android.imagic.files.appwidget.toAuthority
import com.android.imagic.files.cloud.sdrive.client.Authority
import com.android.imagic.files.cloud.sdrive.client.StarBoardDriveClient.OkHttpAuthenticatorInterceptor
import com.android.imagic.files.util.ApiOperation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.logging.HttpLoggingInterceptor

class WeChatLoginViewModel : ViewModel() {

    private val _wechatQRCodeUrlLiveData = MutableLiveData<String?>(null)
    val wechatQRCodeUrlLiveData: LiveData<String?> = _wechatQRCodeUrlLiveData

    private val _authorityLiveData = MutableLiveData<Authority?>(null)
    val authorityLiveData: LiveData<Authority?> = _authorityLiveData

    val loginClient = CloudClient(okHttpClient.newBuilder().build())

    private var reloadJob: Job? = null

    fun getLoginUrl() = viewModelScope.launch {
        loginClient.getLoginUrlInfo(3)
            .onSuccess { loginUrlInfo ->
                Log.d("chengluo", "getLoginUrl Success: $loginUrlInfo")
                _wechatQRCodeUrlLiveData.value = loginUrlInfo.loginURL
            }
            .onFailure {
                Log.e("chengluo", "getLoginUrl Failure: ", it)
            }
    }

    fun doLogin(code: String, state: String) = viewModelScope.launch {
        loginClient.doLogin(code, state)
            .onSuccess { loginInfo ->
                Log.d("chengluo", "doLogin Success: $loginInfo")
                getSpaceUsageInfo(loginInfo.toAuthority())
            }.onFailure { exception ->
                Log.e("chengluo", "doLogin Failure: ", exception)
            }
    }

    private fun getSpaceUsageInfo(authority: Authority) = viewModelScope.launch {
        val authenticatorInterceptor = OkHttpAuthenticatorInterceptor(authority)
        val okHttpClient = okHttpClient.newBuilder()
            .addNetworkInterceptor(authenticatorInterceptor)
            .authenticator(authenticatorInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
        val nweAuthority = when (val response = CloudClient(okHttpClient).getSpaceUsageInfo()) {
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
        _authorityLiveData.value = nweAuthority
    }

    fun reLoadQRCode() {
        _wechatQRCodeUrlLiveData.value = null
        getLoginUrl()
    }

    fun startScheduledReload() {
        Log.d("chengluo", "startScheduledReload: ")
        reloadJob?.cancel()
        reloadJob = null
        reloadJob = viewModelScope.launch {
            delay(275 * 1000)
            reLoadQRCode()
        }
    }

    fun stopScheduledReload() {
        Log.d("chengluo", "stopScheduledReload: ")
        reloadJob?.cancel()
        reloadJob = null
    }
}
