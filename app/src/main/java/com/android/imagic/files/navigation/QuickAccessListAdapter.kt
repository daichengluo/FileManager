package com.android.imagic.files.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.RecyclerView
import com.android.imagic.files.compat.foregroundCompat
import com.android.imagic.files.compat.obtainStyledAttributesCompat
import com.android.imagic.files.compat.use
import com.android.imagic.files.databinding.NavigationQuickAccessItemBinding
import com.android.imagic.files.ui.AutoMirrorDrawable
import com.android.imagic.files.ui.SimpleAdapter
import com.android.imagic.files.util.layoutInflater
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class QuickAccessListAdapter(
    private val listener: NavigationItem.Listener,
    context: Context
) : SimpleAdapter<NavigationItem?, RecyclerView.ViewHolder>() {
    @SuppressLint("PrivateResource", "RestrictedApi")
    private val viewAttributes = context.obtainStyledAttributesCompat(
        null, com.google.android.material.R.styleable.NavigationView,
        com.google.android.material.R.attr.navigationViewStyle,
        com.google.android.material.R.style.Widget_MaterialComponents_NavigationView
    ).use { a ->
        val itemShapeAppearance = a.getResourceId(
            com.google.android.material.R.styleable.NavigationView_itemShapeAppearance, 0
        )
        val itemShapeAppearanceOverlay = a.getResourceId(
            com.google.android.material.R.styleable.NavigationView_itemShapeAppearanceOverlay, 0
        )
        val itemShapeFillColor = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked), // 选中状态
                intArrayOf() // 默认状态（未选中）
            ),
            intArrayOf(
                0xFFEFF0F7.toInt(), // 选中时的颜色（#FFEFF0F7）
                0x00000000  // 未选中时的颜色（透明或默认颜色）
            )
        )
        val itemBackground = createItemShapeDrawable(
            itemShapeAppearance, itemShapeAppearanceOverlay, itemShapeFillColor, context
        )
        val controlHighlightColor = ColorStateList.valueOf(0x4D000000)
        val itemForegroundMaskFillColor = ColorStateList.valueOf(Color.WHITE)
        val itemForegroundMask = createItemShapeDrawable(
            itemShapeAppearance, itemShapeAppearanceOverlay, itemForegroundMaskFillColor, context
        )
        val itemForeground = RippleDrawable(controlHighlightColor, null, itemForegroundMask)
        ViewAttributes(
            itemBackground,
            itemForeground
        )
    }

    // @see com.google.android.material.navigation.NavigationView#createDefaultItemBackground
    private fun createItemShapeDrawable(
        @StyleRes shapeAppearance: Int,
        @StyleRes shapeAppearanceOverlay: Int,
        fillColor: ColorStateList?,
        context: Context
    ): Drawable {
        val shapeAppearanceModel =
            ShapeAppearanceModel.builder(context, shapeAppearance, shapeAppearanceOverlay)
                .setAllCorners(CornerFamily.ROUNDED, 30f)
                .build()
        val materialShapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)
            .apply { this.fillColor = fillColor }
        return AutoMirrorDrawable(
            InsetDrawable(materialShapeDrawable, 0, 0, 0, 0)
        )
    }

    fun notifyCheckedChanged() {
        notifyItemRangeChanged(0, itemCount, PAYLOAD_CHECKED_CHANGED)
    }

    override val hasStableIds: Boolean
        get() = true

    override fun getItemId(position: Int): Long =
        getItem(position)?.id ?: list.subList(0, position).count { it == null }.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ItemHolder(
            NavigationQuickAccessItemBinding.inflate(parent.context.layoutInflater, parent, false)
        ).apply {
            binding.itemLayout.background =
                viewAttributes.itemBackground?.constantState?.newDrawable()
            binding.itemLayout.foregroundCompat =
                viewAttributes.itemForeground?.constantState?.newDrawable()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        val item = getItem(position)!!
        val binding = (holder as ItemHolder).binding
        binding.itemLayout.isChecked = item.isChecked(listener)
        if (payloads.isNotEmpty()) {
            return
        }
        binding.itemLayout.setOnClickListener { item.onClick(listener) }
        binding.itemLayout.setOnLongClickListener { item.onLongClick(listener) }
        binding.iconImage.setImageDrawable(item.getIcon(binding.iconImage.context))
        binding.titleText.text = item.getTitle(binding.titleText.context)
    }

    companion object {
        private val PAYLOAD_CHECKED_CHANGED = Any()
    }

    private class ViewAttributes(
        val itemBackground: Drawable?,
        val itemForeground: Drawable?
    )

    private class ItemHolder(val binding: NavigationQuickAccessItemBinding) :
        RecyclerView.ViewHolder(
        binding.root
    )
}
