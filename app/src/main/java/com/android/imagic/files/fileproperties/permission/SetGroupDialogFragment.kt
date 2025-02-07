package com.android.imagic.files.fileproperties.permission

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.file.FileItem
import com.android.imagic.files.filejob.FileJobService
import com.android.imagic.files.provider.common.PosixFileAttributes
import com.android.imagic.files.provider.common.PosixGroup
import com.android.imagic.files.provider.common.toByteString
import com.android.imagic.files.util.SelectionLiveData
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.show
import com.android.imagic.files.util.viewModels
import java8.nio.file.Path

class SetGroupDialogFragment : SetPrincipalDialogFragment() {
    override val viewModel: SetPrincipalViewModel by viewModels { { SetGroupViewModel() } }

    @StringRes
    override val titleRes: Int = R.string.file_properties_permission_set_group_title

    override fun createAdapter(selectionLiveData: SelectionLiveData<Int>): PrincipalListAdapter =
        GroupListAdapter(selectionLiveData)

    override val PosixFileAttributes.principal
        get() = group()!!

    override fun setPrincipal(path: Path, principal: PrincipalItem, recursive: Boolean) {
        val group = PosixGroup(principal.id, principal.name?.toByteString())
        FileJobService.setGroup(path, group, recursive, requireContext())
    }

    companion object {
        fun show(file: FileItem, fragment: Fragment) {
            SetGroupDialogFragment().putArgs(Args(file)).show(fragment)
        }
    }
}
