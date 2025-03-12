package com.android.imagic.files.ui

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class MaxHeightRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {
    private var maxHeight = 0

    fun setMaxHeight(height: Int) {
        maxHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxHeightSpec = if (maxHeight > 0) {
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        } else {
            heightMeasureSpec
        }
        super.onMeasure(widthMeasureSpec, maxHeightSpec)
    }
}
