package com.android.imagic.files.filelist

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.imagic.files.R
import com.android.imagic.files.databinding.FileTransferItemBinding
import com.android.imagic.files.filejob.FileJobReceiver
import com.android.imagic.files.filejob.FileJobService.Actions
import com.android.imagic.files.filejob.FileJobService.Companion.FILE_TRANSFER_ID
import com.android.imagic.files.settings.Settings
import com.android.imagic.files.ui.AnimatedListAdapter
import com.android.imagic.files.ui.CheckableForegroundLinearLayout
import com.android.imagic.files.util.layoutInflater
import com.android.imagic.files.util.valueCompat
import me.zhanghai.android.foregroundcompat.ForegroundImageButton

class FileTransferAdapter(
    private val listener: Listener
) : AnimatedListAdapter<FileTransfer, FileTransferAdapter.ViewHolder>(CALLBACK) {

    private val transferPositionMap = mutableMapOf<Int, Int>()

    override fun clear() {
        super.clear()
        rebuildFilePositionMap()
    }

    fun updateItem(transfer: FileTransfer) {
        val list = list
        var found = false
        val newList = list.map {
            if (it.id == transfer.id) {
                found = true
                transfer
            } else it
        }
        replace(if (found) newList else newList + transfer, false)
    }

    fun removeItem(id: Int) {
        val list = list
        val newList = list.filterNot { it.id == id }
        replace(newList, false)
    }

    override fun replace(list: List<FileTransfer>, clear: Boolean) {
        super.replace(list, clear)
        rebuildFilePositionMap()
        if (list.isEmpty()) {
            listener.hideTransfer()
        } else {
            listener.showTransfer()
        }
    }

    private fun rebuildFilePositionMap() {
        transferPositionMap.clear()
        for (index in 0..<itemCount) {
            val transferProgress = getItem(index)
            transferPositionMap[transferProgress.id] = index
        }
    }

    fun cancelAllTransferJob(context: Context) {
        val list = list
        list.forEach {
            FileJobReceiver.createIntent(it.id).also {
                context.sendBroadcast(it)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = parent.context.layoutInflater
        val holder = ViewHolder(FileTransferItemBinding.inflate(inflater, parent, false))
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val transfer = getItem(position)
        val id = transfer.id
        val status = transfer.status
        holder.itemLayout.apply {
            if (status == FileTransfer.Status.FAILED) {
                background = AppCompatResources.getDrawable(
                    context,
                    R.drawable.file_transfer_failed_background
                )
            }
        }
        holder.titleText.apply {
            if (text != transfer.title) {
                text = transfer.title
            }
        }
        holder.subText.apply {
            text = if (status == FileTransfer.Status.CANCELLING) {
                context.getString(R.string.file_transfer_cancelling)
            } else {
                transfer.text
            }
        }
        holder.progressbar.apply {
            if (status == FileTransfer.Status.CANCELLING) {
                isIndeterminate = true
            } else {
                max = transfer.max
                progress = transfer.progress
                isIndeterminate = transfer.indeterminate
            }
        }
        holder.cancelButton.apply {
            if (status == FileTransfer.Status.CANCELLING) {
                isEnabled = false
            } else {
                isEnabled = true
                setOnClickListener {
                    Intent(Actions.CANCELLING.toString()).apply {
                        putExtra(FILE_TRANSFER_ID, id)
                    }.also {
                        LocalBroadcastManager.getInstance(context).sendBroadcast(it)
                    }
                    FileJobReceiver.createIntent(id).also {
                        context.sendBroadcast(it)
                    }
                }
            }
        }
    }

    override val isAnimationEnabled: Boolean
        get() = Settings.FILE_LIST_ANIMATION.valueCompat

    companion object {
        private val PAYLOAD_STATE_CHANGED = Any()

        private val CALLBACK = object : DiffUtil.ItemCallback<FileTransfer>() {
            override fun areItemsTheSame(
                oldItem: FileTransfer,
                newItem: FileTransfer
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: FileTransfer,
                newItem: FileTransfer
            ): Boolean = oldItem == newItem
        }
    }

    class ViewHolder private constructor(
        root: View,
        val itemLayout: CheckableForegroundLinearLayout,
        val titleText: TextView,
        val subText: TextView,
        val progressbar: ProgressBar,
        val cancelButton: ForegroundImageButton
    ) : RecyclerView.ViewHolder(root) {
        constructor(binding: FileTransferItemBinding) : this(
            binding.root,
            binding.itemLayout,
            binding.titleText,
            binding.subText,
            binding.progressbar,
            binding.cancelButton
        )
    }

    interface Listener {
        fun showTransfer()
        fun hideTransfer()
    }
}
