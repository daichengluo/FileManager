package com.android.imagic.files.storage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.imagic.files.util.valueCompat

class EditStarBoardDriveServerViewModel : ViewModel() {

    private val _userAgreementCheckedLiveData = MutableLiveData<Boolean>(false)
    val userAgreementCheckedLiveData: LiveData<Boolean> = _userAgreementCheckedLiveData
    val userAgreementChecked: Boolean
        get() = userAgreementCheckedLiveData.valueCompat

    fun updateUserAgreementChecked() {
        val checked = _userAgreementCheckedLiveData.valueCompat
        _userAgreementCheckedLiveData.value = !checked
    }
}
