package com.android.imagic.files.fileproperties.permission

import androidx.annotation.DrawableRes
import com.android.imagic.files.R
import com.android.imagic.files.util.SelectionLiveData

class UserListAdapter(
    selectionLiveData: SelectionLiveData<Int>
) : PrincipalListAdapter(selectionLiveData) {
    @DrawableRes
    override val principalIconRes: Int = R.drawable.person_icon_control_normal_24dp
}
