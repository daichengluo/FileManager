package com.android.imagic.files.storage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.imagic.files.cloud.sdrive.client.Authority
import kotlinx.coroutines.launch

class WechatQRCodeLoginViewModel : ViewModel() {
    private val _wxQrcodeUrl = MutableLiveData<String?>(null)
    val wxQrcodeUrl: LiveData<String?> = _wxQrcodeUrl

    private val _loginSuccess = MutableLiveData<Boolean>(false)
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    fun getLoginUrl() = viewModelScope.launch {
        SDriveAuthClient.getLoginUrlInfo(3)
            .onSuccess { response ->
                _wxQrcodeUrl.value = response.data?.loginURL
            }
            .onFailure {
                it.printStackTrace()
            }
    }

    fun doLogin(code: String, state: String) = viewModelScope.launch {
        SDriveAuthClient.doLogin(code, state)
            .onSuccess { response ->
                response.data?.let {
                    val authority = Authority(
                        it.openid,
                        it.access_token,
                        it.id,
                        it.nickname,
                        it.headimgurl
                    )
                    val server = SDriveServer(null, null, authority)
                    Storages.addOrReplace(server)
                    _loginSuccess.value = true
                }
            }
            .onFailure {
                _wxQrcodeUrl.value = null
                getLoginUrl()
            }
    }
}
