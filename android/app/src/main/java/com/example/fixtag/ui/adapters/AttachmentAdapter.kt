package com.example.fixtag.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.dispose
import coil3.load
import com.example.fixtag.R
import com.example.fixtag.data.model.Attachment
import com.example.fixtag.data.model.AttachmentKind
import com.example.fixtag.data.model.SyncStatus
import java.io.File

data class AttachmentItem(
    val id: String,
    val kind: AttachmentKind,
    val path: String?,  // null until the file is on this device
)

fun Attachment.toItem() = AttachmentItem(
    id = id.toString(),
    kind = kind,
    path = path.takeIf { fetchStatus == SyncStatus.Synced },
)

class AttachmentAdapter(
    private val onOpen: (AttachmentItem) -> Unit,
    private val onRemove: ((AttachmentItem) -> Unit)? = null,  // only the new incident form passes this
) : ListAdapter<AttachmentItem, AttachmentAdapter.ViewHolder>(Diff) {

    var playingPath: String? = null
        set(value) { field = value; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_attachment, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val remove: View = itemView.findViewById(R.id.remove)

        fun bind(item: AttachmentItem) {
            val path = item.path
            if (item.kind == AttachmentKind.Photo && path != null) {
                thumbnail.load(File(path))
            } else {
                thumbnail.dispose()  // a recycled tile may still be loading another photo
                thumbnail.setImageDrawable(null)
            }
            val iconRes = when {
                path == null -> R.drawable.ic_download
                item.kind == AttachmentKind.Photo -> null
                path == playingPath -> R.drawable.ic_stop
                else -> R.drawable.ic_play
            }
            icon.isVisible = iconRes != null
            iconRes?.let(icon::setImageResource)
            remove.isVisible = onRemove != null
            remove.setOnClickListener { onRemove?.invoke(item) }
            itemView.setOnClickListener { if (path != null) onOpen(item) }
        }
    }

    object Diff : DiffUtil.ItemCallback<AttachmentItem>() {
        override fun areItemsTheSame(old: AttachmentItem, new: AttachmentItem) = old.id == new.id
        override fun areContentsTheSame(old: AttachmentItem, new: AttachmentItem) = old == new
    }
}