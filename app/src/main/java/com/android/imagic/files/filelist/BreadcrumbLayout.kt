package com.android.imagic.files.filelist

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.android.imagic.files.R
import com.android.imagic.files.databinding.BreadcrumbItemBinding
import com.android.imagic.files.util.getDimensionPixelSize
import com.android.imagic.files.util.layoutInflater
import com.android.imagic.files.util.withTheme
import java8.nio.file.Path

class BreadcrumbLayout : HorizontalScrollView {
    private val tabLayoutHeight = context.getDimensionPixelSize(R.dimen.tab_layout_height)

    // Using a color state list resource somehow results in red color in dark mode on API 21.
    // Run `git revert 5bb2fd1` once we no longer support API 21.
    private val itemColor = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_activated), intArrayOf()),
        intArrayOf(
            0xFF262626.toInt(),
            0x80262626.toInt()
        )
    )
    private val popupContext = context.withTheme(R.style.ActionBarPopupTheme)

    private val itemsLayout: LinearLayout

    private lateinit var listener: Listener
    private lateinit var data: BreadcrumbData

    private var isLayoutDirty = true
    private var isScrollToSelectedItemPending = false
    private var isFirstScroll = true

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    )

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int,
        @StyleRes defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        isHorizontalScrollBarEnabled = false
        itemsLayout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        itemsLayout.setPaddingRelative(paddingStart, paddingTop, paddingEnd, paddingBottom)
        setPaddingRelative(0, 0, 0, 0)
        addView(itemsLayout, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
    }

    override fun jumpDrawablesToCurrentState() {
        // HACK: AppBarLayout.updateAppBarLayoutDrawableState() calls
        // CoordinatorLayout.jumpDrawablesToCurrentState() to fix a pre-N visual bug according to a
        // comment in AppBarLayout.BaseBehavior.onLayoutChild(), however that results in our ripple
        // disappearing. One way to ignore that call path is to skip when we are in layout, so that
        // we at least preserve the other call path upon being attached to window.
        if (isInLayout) {
            return
        }
        super.jumpDrawablesToCurrentState()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val newHeightMeasureSpec = if (heightMode != MeasureSpec.EXACTLY) {
            val maximumHeight = if (heightMode == MeasureSpec.AT_MOST) {
                MeasureSpec.getSize(heightMeasureSpec)
            } else {
                Int.MAX_VALUE
            }
            val height = tabLayoutHeight.coerceAtMost(maximumHeight)
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        } else {
            heightMeasureSpec
        }
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }

    override fun requestLayout() {
        isLayoutDirty = true

        super.requestLayout()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        isLayoutDirty = false
        if (isScrollToSelectedItemPending) {
            scrollToSelectedItem()
            isScrollToSelectedItemPending = false
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setData(data: BreadcrumbData) {
        if (this::data.isInitialized && this.data == data) {
            return
        }
        this.data = data
        inflateItemViews()
        bindItemViews()
        scrollToSelectedItem()
    }

    private fun scrollToSelectedItem() {
        if (isLayoutDirty) {
            isScrollToSelectedItemPending = true
            return
        }
        val selectedItemView = itemsLayout.getChildAt(data.selectedIndex)
        val scrollX = if (layoutDirection == LAYOUT_DIRECTION_LTR) {
            selectedItemView.left - itemsLayout.paddingStart
        } else {
            selectedItemView.right - width + itemsLayout.paddingStart
        }
        if (!isFirstScroll && isShown) {
            smoothScrollTo(scrollX, 0)
        } else {
            scrollTo(scrollX, 0)
        }
        isFirstScroll = false
    }

    private fun inflateItemViews() {
        val size = data.cloudPaths?.size ?: data.paths.size

        for (i in itemsLayout.childCount - 1 downTo size) {
            itemsLayout.removeViewAt(i)
        }

        for (i in itemsLayout.childCount until size) {
            val binding = BreadcrumbItemBinding.inflate(context.layoutInflater, itemsLayout, false)

            binding.text.setTextColor(itemColor)
            binding.arrowImage.imageTintList = itemColor
            binding.root.tag = binding
            itemsLayout.addView(binding.root)
        }
    }

    private fun bindItemViews() {
        val cloudPaths = data.cloudPaths
        if (cloudPaths != null) {
            for (index in cloudPaths.indices) {
                @Suppress("UNCHECKED_CAST")
                val binding = itemsLayout.getChildAt(index).tag as BreadcrumbItemBinding
                binding.text.text = data.cloudPaths!![index].name
                binding.arrowImage.isVisible = index < data.selectedIndex
                binding.root.isActivated = index == data.selectedIndex
                binding.root.isEnabled = index != data.selectedIndex
                binding.root.isVisible = index <= data.selectedIndex
                val cloudPath = data.cloudPaths!![index]
                binding.root.setOnClickListener {
                    if (data.selectedIndex == index) {
                        scrollToSelectedItem()
                    } else {
                        listener.navigateCloudTo(cloudPath)
                    }
                }
            }
            return
        }

        for (index in data.paths.indices) {
            @Suppress("UNCHECKED_CAST")
            val binding = itemsLayout.getChildAt(index).tag as BreadcrumbItemBinding
            binding.text.text = data.nameProducers[index](binding.text.context)
            binding.arrowImage.isVisible = index < data.selectedIndex
            binding.root.isActivated = index == data.selectedIndex
            binding.root.isEnabled = index != data.selectedIndex
            binding.root.isVisible = index <= data.selectedIndex
            val path = data.paths[index]
            binding.root.setOnClickListener {
                if (data.selectedIndex == index) {
                    scrollToSelectedItem()
                } else {
                    listener.navigateTo(path)
                }
            }
        }
    }

    interface Listener {
        fun navigateTo(path: Path)
        fun navigateCloudTo(cloudPathData: CloudPathData)
        fun copyPath(path: Path)
        fun openInNewTask(path: Path)
    }
}
