
package com.android.imagic.files.filelist

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.android.imagic.files.R
import com.android.imagic.files.util.ParcelableArgs
import com.android.imagic.files.util.ParcelableParceler
import com.android.imagic.files.util.args
import com.android.imagic.files.util.putArgs
import com.android.imagic.files.util.show
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

class NavigateToPathDialogFragment : PathDialogFragment() {
    private val args by args<Args>()

    override val listener: Listener
        get() = super.listener as Listener

    @StringRes
    override val titleRes: Int = R.string.file_list_navigate_to_title

    override val initialName: String?
        get() = args.path.toUserFriendlyString()

    override fun onOk(path: Path) {
        listener.navigateTo(path)
    }

    companion object {
        fun show(path: Path, fragment: Fragment) {
            NavigateToPathDialogFragment().putArgs(Args(path)).show(fragment)
        }
    }

    @Parcelize
    class Args(val path: @WriteWith<ParcelableParceler> Path) : ParcelableArgs

    interface Listener : NameDialogFragment.Listener {
        fun navigateTo(path: Path)
    }
}
