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
import com.android.imagic.files.coil.AppIconPackageName
import com.android.imagic.files.compat.foregroundCompat
import com.android.imagic.files.compat.getDrawableCompat
import com.android.imagic.files.compat.isSingleLineCompat
import com.android.imagic.files.databinding.FileItemGridBinding
import com.android.imagic.files.databinding.FileItemListBinding
import com.android.imagic.files.file.FileItem
import com.android.imagic.files.file.fileSize
import com.android.imagic.files.file.formatShort
import com.android.imagic.files.file.iconRes
import com.android.imagic.files.file.isApk
import com.android.imagic.files.provider.archive.isArchivePath
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.ui.AnimatedListAdapter
import com.android.imagic.files.ui.CheckableForegroundLinearLayout
import com.android.imagic.files.ui.CheckableItemBackground
import com.android.imagic.files.util.getQuantityString
import com.android.imagic.files.util.isMaterial3Theme
import com.android.imagic.files.util.layoutInflater
import com.android.imagic.files.util.valueCompat
import java8.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.zhanghai.android.fastscroll.PopupTextProvider
import java.util.Locale
import kotlin.math.abs

class FileListAdapter(
    private val listener: Listener
) : AnimatedListAdapter<FileItem, FileListAdapter.ViewHolder>(CALLBACK), PopupTextProvider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isSearching = false
    private var isShowHiddenFiles = false

    private lateinit var _viewType: FileViewType
    var viewType: FileViewType
        get() = _viewType
        set(value) {
            _viewType = value
            if (!isSearching) {
                super.replace(list, true)
            }
        }

    private lateinit var _sortOptions: FileSortOptions
    var sortOptions: FileSortOptions
        get() = _sortOptions
        set(value) {
            _sortOptions = value
            if (!isSearching) {
                val sortedList = list.sortedWith(value.createComparator())
                super.replace(sortedList, true)
                rebuildFilePositionMap()
            }
        }

    var pickOptions: PickOptions? = null
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
        }

    private val selectedFiles = fileItemSetOf()

    private val filePositionMap = mutableMapOf<Path, Int>()

    private lateinit var _nameEllipsize: TextUtils.TruncateAt
    var nameEllipsize: TextUtils.TruncateAt
        get() = _nameEllipsize
        set(value) {
            _nameEllipsize = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
        }

    fun replaceSelectedFiles(files: FileItemSet) {
        val changedFiles = fileItemSetOf()
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
            val position = filePositionMap[file.path]
            position?.let { notifyItemChanged(it, PAYLOAD_STATE_CHANGED) }
        }
    }

    fun clearSelectedFiles() {
        notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
    }

    private fun selectFile(file: FileItem) {
        if (!isFileSelectable(file)) {
            return
        }
        val selected = file in selectedFiles
        val pickOptions = pickOptions
        if (!selected && pickOptions != null && !pickOptions.allowMultiple) {
            listener.clearSelectedFiles()
        }
        listener.selectFile(file, !selected)
        notifyItemRangeChanged(0, itemCount, PAYLOAD_STATE_CHANGED)
    }

    fun selectAllFiles() {
        val files = fileItemSetOf()
        for (index in 0..<itemCount) {
            val file = getItem(index)
            if (isFileSelectable(file)) {
                files.add(file)
            }
        }
        listener.selectFiles(files, true)
    }

    private fun isFileSelectable(file: FileItem): Boolean {
        val pickOptions = pickOptions ?: return true
        return when (pickOptions.mode) {
            PickOptions.Mode.OPEN_FILE, PickOptions.Mode.CREATE_FILE ->
                !file.attributes.isDirectory &&
                        pickOptions.mimeTypes.any { it.match(file.mimeType) }

            PickOptions.Mode.OPEN_DIRECTORY -> file.attributes.isDirectory
        }
    }

    override fun clear() {
        super.clear()

        rebuildFilePositionMap()
    }

    @Deprecated("", ReplaceWith("replaceListAndSearching(list, searching)"))
    override fun replace(list: List<FileItem>, clear: Boolean) {
        throw UnsupportedOperationException()
    }

    fun replaceListAndIsSearching(
        list: List<FileItem>,
        isSearching: Boolean,
        isShowHiddenFiles: Boolean
    ) {
        val clear = this.isSearching != isSearching
        this.isSearching = isSearching
        this.isShowHiddenFiles = isShowHiddenFiles
        val sortedList = if (!isSearching) list.sortedWith(sortOptions.createComparator()) else list
        super.replace(sortedList, clear)
        rebuildFilePositionMap()
    }

    private fun rebuildFilePositionMap() {
        filePositionMap.clear()
        for (index in 0..<itemCount) {
            val file = getItem(index)
            filePositionMap[file.path] = index
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
                    .apply { inflate(R.menu.file_item) }
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
        val isDirectory = file.attributes.isDirectory
        val isEnabled = isFileSelectable(file) || isDirectory
        holder.itemLayout.isEnabled = isEnabled
        holder.menuButton?.isEnabled = isEnabled
        holder.menuButton?.isVisible = selectedFiles.isEmpty() && _viewType == FileViewType.LIST
        holder.checkImage.isVisible = selectedFiles.isNotEmpty()
        val menu = holder.popupMenu?.menu
        val path = file.path
        val hasPickOptions = pickOptions != null
        val isReadOnly = path.fileSystem.isReadOnly
        menu?.findItem(R.id.action_cut)?.isVisible = !hasPickOptions && !isReadOnly
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
                    listener.cancelDragDrop()
                    holder.itemLayout.isChecked = false
                    true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    true
                }

                DragEvent.ACTION_DROP -> {
                    if (isDirectory) {
                        listener.dropFileTo(path)
                    }
                    true
                }

                else -> false
            }
        }
        holder.itemLayout.apply {
            setOnClickListener {
                if (selectedFiles.isEmpty()) {
                    listener.openFile(file)
                } else {
                    selectFile(file)
                }
            }
            setOnLongClickListener {
                if (selectedFiles.isEmpty()) {
                    selectFile(file)
                }/* else {
                    listener.openFile(file)
                }*/
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
        val supportsThumbnail = file.supportsThumbnail
        val shouldLoadThumbnailIcon = supportsThumbnail && holder.thumbnailIconImage != null &&
                file.mimeType.isApk
        val attributes = file.attributes
        holder.thumbnailIconImage?.apply {
            dispose()
            isVisible = !isDirectory
            setImageResource(iconRes)
            if (shouldLoadThumbnailIcon) {
                load(path to attributes)
            }
        }
        holder.thumbnailImage.apply {
            dispose()
            setImageDrawable(null)
            val shouldLoadThumbnail = supportsThumbnail && !shouldLoadThumbnailIcon
            isVisible = shouldLoadThumbnail
            if (shouldLoadThumbnail) {
                load(path to attributes) {
                    listener { _, _ ->
                        val iconImage = holder.thumbnailIconImage ?: holder.iconImage
                        iconImage?.isVisible = false
                    }
                }
            }
        }
        holder.appIconBadgeImage?.apply {
            dispose()
            setImageDrawable(null)
            val appDirectoryPackageName = file.appDirectoryPackageName
            val hasAppIconBadge = appDirectoryPackageName != null
            isVisible = hasAppIconBadge
            if (hasAppIconBadge) {
                load(AppIconPackageName(appDirectoryPackageName!!))
            }
        }
        holder.nameText.text = file.name
        if (isDirectory) {
            holder.descriptionText?.let {
                val context = holder.descriptionText.context
                val lastModificationTime = attributes.lastModifiedTime().toInstant()
                    .formatShort(context)

                scope.launch {
                    file.getFolderItemCount(isShowHiddenFiles) { count ->
                        val fileCountText = context.getQuantityString(
                            R.plurals.file_item_file_count_format, count, count
                        )
                        val descriptionSeparator =
                            context.getString(R.string.file_item_description_separator)
                        holder.descriptionText.text =
                            listOf(lastModificationTime, fileCountText).joinToString(
                                descriptionSeparator
                            )
                    }
                }
            }
        } else {
            holder.descriptionText?.let {
                val context = holder.descriptionText.context
                val lastModificationTime = attributes.lastModifiedTime().toInstant()
                    .formatShort(context)
                val size = attributes.fileSize.formatHumanReadable(context)
                val descriptionSeparator =
                    context.getString(R.string.file_item_description_separator)
                holder.descriptionText.text =
                    listOf(lastModificationTime, size).joinToString(descriptionSeparator)
            }
        }
        val isArchivePath = path.isArchivePath
        menu?.findItem(R.id.action_copy)
            ?.setTitle(if (isArchivePath) R.string.file_item_action_extract else R.string.copy)
        menu?.findItem(R.id.action_delete)?.isVisible = !isReadOnly
        menu?.findItem(R.id.action_rename)?.isVisible = !isReadOnly
        menu?.findItem(R.id.action_extract)?.isVisible = file.isArchiveFile
        menu?.findItem(R.id.action_archive)?.isVisible = !isArchivePath
        menu?.findItem(R.id.action_add_bookmark)?.isVisible = isDirectory
        holder.popupMenu?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_open_with -> {
                    listener.openFileWith(file)
                    true
                }

                R.id.action_cut -> {
                    listener.cutFile(file)
                    true
                }

                R.id.action_copy -> {
                    listener.copyFile(file)
                    true
                }

                R.id.action_delete -> {
                    listener.confirmDeleteFile(file)
                    true
                }

                R.id.action_rename -> {
                    listener.showRenameFileDialog(file)
                    true
                }

                R.id.action_extract -> {
                    listener.extractFile(file)
                    true
                }

                R.id.action_archive -> {
                    listener.showCreateArchiveDialog(file)
                    true
                }

                R.id.action_share -> {
                    listener.shareFile(file)
                    true
                }

                R.id.action_copy_path -> {
                    listener.copyPath(file)
                    true
                }

                R.id.action_add_bookmark -> {
                    listener.addBookmark(file)
                    true
                }

                R.id.action_create_shortcut -> {
                    listener.createShortcut(file)
                    true
                }

                R.id.action_properties -> {
                    listener.showPropertiesDialog(file)
                    true
                }

                else -> false
            }
        }
    }

    override fun getPopupText(view: View, position: Int): CharSequence {
        val file = getItem(position)
        return when (sortOptions.by) {
            FileSortOptions.By.NAME -> file.name.take(1).uppercase(Locale.getDefault())
            FileSortOptions.By.TYPE -> file.extension.uppercase(Locale.getDefault())
            FileSortOptions.By.SIZE -> file.attributes.fileSize.formatHumanReadable(view.context)
            FileSortOptions.By.LAST_MODIFIED ->
                file.attributes.lastModifiedTime().toInstant().formatShort(view.context)
        }
    }

    override val isAnimationEnabled: Boolean
        get() = Settings.FILE_LIST_ANIMATION.valueCompat

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnFileDragListener(view: View, file: FileItem, delayMillis: Long = 500) {
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

    private fun startDragAndDrop(file: FileItem, view: View) {
        // drag moved clearSelectedFiles
        listener.clearSelectedFiles()
        clearSelectedFiles()

        // copy file
        listener.copyFile(file)

        val data = ClipData.newPlainText("filePath", file.path.toString())
        val shadowView = listener.getDragShadow(file)
        val dragShadow = View.DragShadowBuilder(shadowView)
        view.startDragAndDrop(data, dragShadow, null, DRAG_FLAG_GLOBAL)
    }

    companion object {
        private val PAYLOAD_STATE_CHANGED = Any()

        private val CALLBACK = object : DiffUtil.ItemCallback<FileItem>() {
            override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean =
                oldItem.path == newItem.path

            override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean =
                oldItem == newItem
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
        val appIconBadgeImage: ImageView?,
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
            null,
            binding.nameText,
            null,
            null,
            binding.checkImage
        )

        var popupMenu: PopupMenu? = null
    }

    interface Listener {
        fun cancelDragDrop()
        fun dropFileTo(targetPath: Path)
        fun getDragShadow(file: FileItem): View
        fun clearSelectedFiles()
        fun selectFile(file: FileItem, selected: Boolean)
        fun selectFiles(files: FileItemSet, selected: Boolean)
        fun openFile(file: FileItem)
        fun openFileWith(file: FileItem)
        fun cutFile(file: FileItem)
        fun copyFile(file: FileItem)
        fun confirmDeleteFile(file: FileItem)
        fun showRenameFileDialog(file: FileItem)
        fun extractFile(file: FileItem)
        fun showCreateArchiveDialog(file: FileItem)
        fun shareFile(file: FileItem)
        fun copyPath(file: FileItem)
        fun addBookmark(file: FileItem)
        fun createShortcut(file: FileItem)
        fun showPropertiesDialog(file: FileItem)
    }
}
