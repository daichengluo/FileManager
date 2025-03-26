package com.android.imagic.files.ui

import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar

class PersistentBarLayoutToolbarActionMode(
    private val persistentBarLayout: PersistentBarLayout,
    private val progressBar: ProgressBar,
    bar: ViewGroup,
    toolbar: Toolbar
) : ToolbarActionMode(bar, toolbar) {
    override fun show(bar: ViewGroup, animate: Boolean) {
        persistentBarLayout.showBar(bar, animate)
    }

    override fun hide(bar: ViewGroup, animate: Boolean) {
        persistentBarLayout.hideBar(bar, animate)
    }

    var progressMax: Int
        get() = progressBar.max
        set(value) {
            progressBar.max = value
        }

    var progress: Int
        get() = progressBar.progress
        set(value) {
            progressBar.progress = value
        }

    var indeterminate: Boolean
        get() = progressBar.isIndeterminate
        set(value) {
            progressBar.setIndeterminate(value)
        }
}
