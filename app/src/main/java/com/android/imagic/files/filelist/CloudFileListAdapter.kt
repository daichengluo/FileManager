package com.android.imagic.files.filelist

import android.annotation.SuppressLint
import android.content.ClipData
import android.text.TextUtils
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.DRAG_FLAG_GLOBAL
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import com.android.imagic.files.R
import com.android.imagic.files.compat.foregroundCompat
import com.android.imagic.files.compat.getDrawableCompat
import com.android.imagic.files.compat.isSingleLineCompat
import com.android.imagic.files.databinding.FileItemGridBinding
import com.android.imagic.files.databinding.FileItemListBinding
import com.android.imagic.files.file.asFileSize
import com.android.imagic.files.file.cloud.sdrive.CloudFileItem
import com.android.imagic.files.file.cloud.sdrive.extension
import com.android.imagic.files.file.cloud.sdrive.toCloudPathData
import com.android.imagic.files.file.cloud.sdrive.toInstant
import com.android.imagic.files.file.formatShort
import com.android.imagic.files.file.iconRes
import com.android.imagic.files.file.isApk
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.ui.AnimatedListAdapter
import com.android.imagic.files.ui.CheckableForegroundLinearLayout
import com.android.imagic.files.ui.CheckableItemBackground
import com.android.imagic.files.util.getQuantityString
import com.android.imagic.files.util.isMaterial3Theme
import com.android.imagic.files.util.layoutInflater
import com.android.imagic.files.util.valueCompat
import me.zhanghai.android.fastscroll.PopupTextProvider
import java.util.Locale
import kotlin.math.abs

class CloudFileListAdapter(
    private val listener: Listener
) : AnimatedListAdapter<CloudFileItem, CloudFileListAdapter.ViewHolder>(CALLBACK),
    PopupTextProvider {

    private lateinit var _viewType: FileViewType
    var viewType: FileViewType
        get() = _viewType
        set(value) {
            _viewType = value
            super.replace(list, true)
        }

    private lateinit var _sortOptions: CloudFileSortOptions
    var sortOptions: CloudFileSortOptions
        get() = _sortOptions
        set(value) {
            _sortOptions = value
            val sortedList = list.sortedWith(value.createComparator())
            super.replace(sortedList, true)
            rebuildFilePositionMap()
        }

    var pickOptions: PickOptions? = null
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
        }

    private val selectedFiles = cloudFileItemSetOf()

    private val filePositionMap = mutableMapOf<Int, Int>()

    private lateinit var _nameEllipsize: TextUtils.TruncateAt
    var nameEllipsize: TextUtils.TruncateAt
        get() = _nameEllipsize
        set(value) {
            _nameEllipsize = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
        }

    fun replaceSelectedFiles(files: CloudFileItemSet) {
        val changedFiles = cloudFileItemSetOf()
        val iterator = selectedFiles.iterator()
        while (iterator.hasNext()) {
            val file = iterator.next()
            if (file !in files) {
                iterator.remove()
                changedFiles.add(file)
            }
        }
        for (file in files) {
            if (file !in selectedFiles) {
                selectedFiles.add(file)
                changedFiles.add(file)
            }
        }
        for (file in changedFiles) {
            val position = filePositionMap[file.id]
            position?.let { notifyItemChanged(it, PAYLOAD_STATE_CHANGED) }
        }
    }

    fun notifyStateChanged() {
        notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
    }

    private fun selectFile(file: CloudFileItem) {
        if (!isFileSelectable(file)) {
            return
        }
        val selected = file in selectedFiles
        val pickOptions = pickOptions
        if (!selected && pickOptions != null && !pickOptions.allowMultiple) {
            listener.clearSelectedCloudFileFiles()
        }
        listener.selectCloudFile(file, !selected)
        notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
    }

    fun selectAllFiles() {
        val files = cloudFileItemSetOf()
        for (index in 0..<itemCount) {
            val file = getItem(index)
            if (isFileSelectable(file)) {
                files.add(file)
            }
        }
        listener.selectCloudFiles(files, true)
    }

    private fun isFileSelectable(file: CloudFileItem): Boolean {
        val pickOptions = pickOptions ?: return true
        return when (pickOptions.mode) {
            PickOptions.Mode.OPEN_FILE, PickOptions.Mode.CREATE_FILE ->
                !file.isDirectory &&
                        pickOptions.mimeTypes.any { it.match(file.mimeType) }

            PickOptions.Mode.OPEN_DIRECTORY -> file.isDirectory
        }
    }

    override fun clear() {
        super.clear()

        rebuildFilePositionMap()
    }

    override fun replace(list: List<CloudFileItem>, clear: Boolean) {
        val sortedList = list.sortedWith(sortOptions.createComparator())
        super.replace(sortedList, clear)
        rebuildFilePositionMap()
    }

    private fun rebuildFilePositionMap() {
        filePositionMap.clear()
        for (index in 0..<itemCount) {
            val file = getItem(index)
            filePositionMap[file.id] = index
        }
    }

    override fun getItemViewType(position: Int): Int = viewType.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val viewType = FileViewType.entries[viewType]
        val inflater = parent.context.layoutInflater
        val holder = when (viewType) {
            FileViewType.LIST -> ViewHolder(FileItemListBinding.inflate(inflater, parent, false))
            FileViewType.GRID -> ViewHolder(FileItemGridBinding.inflate(inflater, parent, false))
        }
        return holder.apply {
            itemLayout.apply {
                val context = context
                val isMaterial3Theme = context.isMaterial3Theme
                foregroundCompat = if (viewType == FileViewType.GRID && isMaterial3Theme) {
                    context.getDrawableCompat(R.drawable.file_item_grid_foreground_material3)
                } else {
                    context.getDrawableCompat(R.drawable.file_item_list_foreground)
                }
                background = if (viewType == FileViewType.GRID && isMaterial3Theme) {
                    CheckableItemBackground.create(4f, 12f, context)
                } else {
                    CheckableItemBackground.create(0f, 8f, context)
                }
            }
            menuButton?.let {
                popupMenu = PopupMenu(menuButton.context, menuButton)
                    .apply { inflate(R.menu.cloud_file_item) }
                menuButton.setOnClickListener { popupMenu?.show() }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        throw UnsupportedOperationException()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val file = getItem(position)
        val isDirectory = file.isDirectory
        val isEnabled = isFileSelectable(file) || isDirectory
        val isGridView = _viewType == FileViewType.LIST

        holder.itemLayout.isEnabled = isEnabled
        holder.menuButton?.isEnabled = isEnabled
        holder.menuButton?.isVisible = selectedFiles.isEmpty() && isGridView
        holder.checkImage.isVisible = selectedFiles.isNotEmpty()

        val menu = holder.popupMenu?.menu
        val hasPickOptions = pickOptions != null
        menu?.findItem(R.id.action_cut)?.isVisible = !hasPickOptions
        menu?.findItem(R.id.action_copy)?.isVisible = !hasPickOptions

        val checked = file in selectedFiles
        holder.itemLayout.isChecked = checked
        holder.nameText.apply {
            if (isSingleLineCompat) {
                val nameEllipsize = nameEllipsize
                ellipsize = nameEllipsize
                isSelected = nameEllipsize == TextUtils.TruncateAt.MARQUEE
            }
        }
        if (payloads.isNotEmpty()) {
            return
        }
        bindViewHolderAnimation(holder)
        holder.itemLayout.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    true
                }

                DragEvent.ACTION_DRAG_ENTERED -> {
                    if (isDirectory) {
                        holder.itemLayout.isChecked = true
                    }
                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    holder.itemLayout.isChecked = false
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    listener.cancelCloudFileDragDrop()
                    holder.itemLayout.isChecked = false
                    true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    true
                }

                DragEvent.ACTION_DROP -> {
                    if (isDirectory) {
                        listener.dropToCloud(file.toCloudPathData!!)
                    }
                    true
                }

                else -> false
            }
        }
        holder.itemLayout.apply {
            setOnClickListener {
                if (selectedFiles.isEmpty()) {
                    listener.openCloudFile(file)
                } else {
                    selectFile(file)
                }
            }
            setOnLongClickListener {
                if (selectedFiles.isEmpty()) {
                    selectFile(file)
                }
                true
            }
            setOnFileDragListener(this, file)
        }
        holder.iconLayout?.setOnClickListener {
            selectFile(file)
        }
        val iconRes = file.mimeType.iconRes
        holder.iconImage?.apply {
            isVisible = true
            setImageResource(iconRes)
        }
        holder.directoryThumbnailImage?.isVisible = isDirectory
        val supportsThumbnail = !file.thumbUrl.isNullOrEmpty()
        val isApk = file.mimeType.isApk
        holder.thumbnailIconImage?.apply {
            dispose()
            isVisible = !isDirectory
            setImageResource(iconRes)
        }
        holder.thumbnailImage.apply {
            dispose()
            setImageDrawable(null)
            isVisible = supportsThumbnail
            if (supportsThumbnail && !isApk) {
                load(file.thumbUrl) {
                    listener { _, _ ->
                        val iconImage = holder.thumbnailIconImage ?: holder.iconImage
                        iconImage?.isVisible = false
                    }
                }
            }
        }
        holder.appIconBadgeImage.apply {
            dispose()
            setImageDrawable(null)
        }
        holder.nameText.text = file.name
        holder.descriptionText?.let {
            val context = it.context
            val childrenCount = file.childrenCount ?: 0
            val descriptionSeparator = context.getString(R.string.file_item_description_separator)
            val lastModificationTime = file.modifyTime?.toInstant()?.formatShort(context) ?: ""

            it.text = if (isDirectory) {
                val childrenCountText = context.getQuantityString(
                    R.plurals.file_item_file_count_format, childrenCount, childrenCount
                )
                listOf(lastModificationTime, childrenCountText).joinToString(descriptionSeparator)
            } else {
                val size = file.size.asFileSize().formatHumanReadable(context)
                listOf(lastModificationTime, size).joinToString(descriptionSeparator)
            }
        }
        holder.popupMenu?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_cut -> {
                    listener.cutCloudFile(file)
                    true
                }

                R.id.action_copy -> {
                    listener.copyCloudFile(file)
                    true
                }

                R.id.action_delete -> {
                    listener.confirmDeleteCloudFile(file)
                    true
                }

                R.id.action_rename -> {
                    listener.showRenameCloudFileDialog(file)
                    true
                }

                R.id.action_properties -> {
                    listener.showCloudFilePropertiesDialog(file)
                    true
                }

                else -> false
            }
        }
    }

    override fun getPopupText(view: View, position: Int): CharSequence {
        val file = getItem(position)
        return when (sortOptions.by) {
            By.NAME -> file.name.take(1).uppercase(Locale.getDefault())
            By.TYPE -> file.extension.uppercase(Locale.getDefault())
            By.SIZE -> file.size.asFileSize().formatHumanReadable(view.context)
            By.LAST_MODIFIED ->
                file.modifyTime?.toInstant()?.formatShort(view.context) ?: ""
        }
    }

    override val isAnimationEnabled: Boolean
        get() = Settings.FILE_LIST_ANIMATION.valueCompat

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnFileDragListener(view: View, file: CloudFileItem, delayMillis: Long = 500) {
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isLongPressTriggered = false
        var isDraggingStarted = false

        val longPressRunnable = Runnable {
            isLongPressTriggered = true
        }

        view.setOnTouchListener { _, event ->
            //if (!isDraggable) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isLongPressTriggered = false
                    isDraggingStarted = false

                    view.postDelayed(longPressRunnable, delayMillis)
                    false
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = abs(event.rawX - initialTouchX)
                    val deltaY = abs(event.rawY - initialTouchY)

                    if (deltaX > 10 || deltaY > 10) {
                        if (isLongPressTriggered && !isDraggingStarted) {
                            isDraggingStarted = true
                            startDragAndDrop(file, view)
                        }
                    }
                    false
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(longPressRunnable)
                    false
                }

                else -> false
            }
        }
    }

    private fun startDragAndDrop(file: CloudFileItem, view: View) {
        // drag moved clearSelectedFiles
        listener.clearSelectedCloudFileFiles()
        notifyStateChanged()

        // copy file
        listener.copyCloudFile(file)

        val data = ClipData.newPlainText("cloudFileId", file.id.toString())
        val shadowView = listener.getCloudFileDragShadow(file)
        val dragShadow = View.DragShadowBuilder(shadowView)
        view.startDragAndDrop(data, dragShadow, null, DRAG_FLAG_GLOBAL)
    }

    companion object {
        private val PAYLOAD_STATE_CHANGED = Any()

        private val CALLBACK = object : DiffUtil.ItemCallback<CloudFileItem>() {
            override fun areItemsTheSame(oldItem: CloudFileItem, newItem: CloudFileItem): Boolean =
                oldItem.id == newItem.id


            override fun areContentsTheSame(
                oldItem: CloudFileItem,
                newItem: CloudFileItem
            ): Boolean = oldItem == newItem
        }
    }

    class ViewHolder private constructor(
        root: View,
        val itemLayout: CheckableForegroundLinearLayout,
        val iconLayout: View?,
        val iconImage: ImageView?,
        val directoryThumbnailImage: ImageView?,
        val thumbnailIconImage: ImageView?,
        val thumbnailImage: ImageView,
        val appIconBadgeImage: ImageView,
        val nameText: TextView,
        val descriptionText: TextView?,
        val menuButton: ImageButton?,
        val checkImage: ImageView
    ) : RecyclerView.ViewHolder(root) {
        constructor(binding: FileItemListBinding) : this(
            binding.root,
            binding.itemLayout,
            binding.iconLayout,
            binding.iconImage,
            null,
            null,
            binding.thumbnailImage,
            binding.appIconBadgeImage,
            binding.nameText,
            binding.descriptionText,
            binding.menuButton,
            binding.checkImage
        )

        constructor(binding: FileItemGridBinding) : this(
            binding.root,
            binding.itemLayout,
            null,
            null,
            binding.directoryThumbnailImage,
            binding.thumbnailIconImage,
            binding.thumbnailImage,
            binding.appIconBadgeImage,
            binding.nameText,
            null,
            null,
            binding.checkImage
        )

        var popupMenu: PopupMenu? = null
    }

    interface Listener {
        fun cancelCloudFileDragDrop()
        fun dropToCloud(cloudDirectoryPath: CloudPathData)
        fun getCloudFileDragShadow(file: CloudFileItem): View
        fun clearSelectedCloudFileFiles()
        fun selectCloudFile(file: CloudFileItem, selected: Boolean)
        fun selectCloudFiles(files: CloudFileItemSet, selected: Boolean)
        fun openCloudFile(file: CloudFileItem)
        fun cutCloudFile(file: CloudFileItem)
        fun copyCloudFile(file: CloudFileItem)
        fun confirmDeleteCloudFile(file: CloudFileItem)
        fun showRenameCloudFileDialog(file: CloudFileItem)
        fun showCloudFilePropertiesDialog(file: CloudFileItem)
    }
}
