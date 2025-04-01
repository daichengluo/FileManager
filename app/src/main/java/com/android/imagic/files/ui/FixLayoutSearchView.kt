package com.android.imagic.files.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.toColorInt
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import com.android.imagic.files.R
import com.android.imagic.files.compat.requireViewByIdCompat

open class FixLayoutSearchView : SearchView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    init {
        // A negative value won't work here because SearchView will use its preferred width as max
        // width instead.
        maxWidth = Int.MAX_VALUE
        val searchBackBtn =
            requireViewByIdCompat<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchBackBtn.setImageResource(R.drawable.action_bar_back_icon)

        val searchEditFrame = requireViewByIdCompat<View>(androidx.appcompat.R.id.search_edit_frame)
        searchEditFrame.updateLayoutParams<MarginLayoutParams> {
            leftMargin = 0
            rightMargin = 0
        }
        val searchSrcText = requireViewByIdCompat<TextView>(androidx.appcompat.R.id.search_src_text)
        searchSrcText.setTextColor("#FF262626".toColorInt())
        searchSrcText.setHintTextColor("#80262626".toColorInt())
        searchSrcText.updatePaddingRelative(start = 0, end = 0)

        val searchCloseBtn = requireViewByIdCompat<ImageView>(
            androidx.appcompat.R.id.search_close_btn
        )
        searchCloseBtn.setImageResource(R.drawable.search_bar_close_icon)
        searchCloseBtn.updateLayoutParams<MarginLayoutParams> {
            topMargin = 36
            bottomMargin = 36
        }
    }
}
