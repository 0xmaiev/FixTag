package com.example.fixtag.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.R
import com.example.fixtag.data.model.IncidentUpdate

class MessageAdapter : ListAdapter<IncidentUpdate, MessageAdapter.ViewHolder>(UserDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_update, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val authorNameTextView: TextView = itemView.findViewById(R.id.authorName)
        private val authorRoleTextView: TextView = itemView.findViewById(R.id.authorRole)
        private val timeElapsedTextView: TextView = itemView.findViewById(R.id.timeElapsed)
        private val messageTextView: TextView = itemView.findViewById(R.id.message)

        fun bind(incidentUpdate: IncidentUpdate) {
            authorNameTextView.text = incidentUpdate.authorName
            val elapsed = DateUtils.getRelativeTimeSpanString( incidentUpdate.createdAt.toEpochMilliseconds())
            timeElapsedTextView.text = elapsed
        }
    }

    // DiffUtil handles smart list updates completely automatically
    object UserDiffCallback : DiffUtil.ItemCallback<IncidentUpdate>() {
        override fun areItemsTheSame(oldItem: IncidentUpdate, newItem: IncidentUpdate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IncidentUpdate, newItem: IncidentUpdate): Boolean {
            return oldItem == newItem
        }
    }
}