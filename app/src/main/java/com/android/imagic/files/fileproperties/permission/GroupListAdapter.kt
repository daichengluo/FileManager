package com.android.imagic.files.fileproperties.permission

import androidx.annotation.DrawableRes
import com.android.imagic.files.R
import com.android.imagic.files.util.SelectionLiveData

class GroupListAdapter(
    selectionLiveData: SelectionLiveData<Int>
) : PrincipalListAdapter(selectionLiveData) {
    @DrawableRes
    override val principalIconRes: Int = R.drawable.people_icon_control_normal_24dp
}
