package com.android.imagic.files.cloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.imagic.files.util.valueCompat

class CloudLoginViewModel : ViewModel() {

    private val _currentLoginMethodLiveData = MutableLiveData(CloudLoginMethod.Password)
    val currentLoginMethodLiveData: LiveData<CloudLoginMethod> = _currentLoginMethodLiveData
    val currentLoginMethod: CloudLoginMethod
        get() = currentLoginMethodLiveData.valueCompat

    private val _userAgreementCheckedLiveData = MutableLiveData(false)
    val userAgreementCheckedLiveData: LiveData<Boolean> = _userAgreementCheckedLiveData
    val userAgreementChecked: Boolean
        get() = userAgreementCheckedLiveData.valueCompat

    fun updateUserAgreementChecked() {
        val checked = _userAgreementCheckedLiveData.valueCompat
        _userAgreementCheckedLiveData.value = !checked
    }

    fun switchLoginMethod() {
        val method = when (currentLoginMethod) {
            CloudLoginMethod.QRCode -> CloudLoginMethod.Password
            CloudLoginMethod.Password -> CloudLoginMethod.QRCode
        }
        _currentLoginMethodLiveData.value = method
    }
}
