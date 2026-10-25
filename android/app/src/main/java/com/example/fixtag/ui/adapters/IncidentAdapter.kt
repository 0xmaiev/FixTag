package com.example.fixtag.ui.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.R
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentAndMachine
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.data.model.SyncStatus
import com.example.fixtag.ui.INCIDENT_ID_KEY
import com.example.fixtag.ui.showSeverity


class IncidentAdapter(
    private val onDelete: ((Incident) -> Unit)? = null,
) : ListAdapter<IncidentAndMachine, IncidentAdapter.ViewHolder>(UserDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_incident, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val modelTextView: TextView = itemView.findViewById(R.id.model)
        private val serialNumberTextView: TextView = itemView.findViewById(R.id.serialNumber)
        private val severityTextView: TextView = itemView.findViewById(R.id.severity)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.description)
        private val statusTextView: TextView = itemView.findViewById(R.id.status)
        private val syncStatusTextView: TextView = itemView.findViewById(R.id.syncStatus)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(data: IncidentAndMachine) {
            modelTextView.text = data.machine.model
            serialNumberTextView.text = data.machine.serialNumber
            deleteButton.isVisible = onDelete != null && data.incident.syncStatus == SyncStatus.Pending
            deleteButton.setOnClickListener { onDelete?.invoke(data.incident) }
            severityTextView.text = data.incident.severity.toString()
            severityTextView.showSeverity(data.incident.severity)
            descriptionTextView.text = data.incident.description
            val status = when(data.incident.syncStatus) {
                SyncStatus.Pending ->  itemView.context.getString(R.string.pending_sync)
                else -> {
                    when(data.incident.status) {
                        IncidentStatus.Received -> itemView.context.getString(R.string.incident_received)
                        IncidentStatus.Analysis -> itemView.context.getString(R.string.incident_analysis)
                        IncidentStatus.Resolved -> itemView.context.getString(R.string.incident_resolved)
                    }
                }
            }
            statusTextView.text = status
            val syncStatus = when(data.incident.syncStatus) {
                SyncStatus.Pending ->  itemView.context.getString(R.string.stored_locally)
                else -> {
                    val elapsed = DateUtils.getRelativeTimeSpanString( data.incident.createdAt.toEpochMilliseconds())
                    elapsed
                }
            }
            syncStatusTextView.text = syncStatus

            itemView.setOnClickListener {
                val bundle = Bundle()
                bundle.putString(INCIDENT_ID_KEY, data.incident.id.toString())
                findNavController(itemView).navigate(
                    R.id.incident_details,
                    bundle
                )
            }
        }
    }

    // DiffUtil handles smart list updates completely automatically
    object UserDiffCallback : DiffUtil.ItemCallback<IncidentAndMachine>() {
        override fun areItemsTheSame(oldItem: IncidentAndMachine, newItem: IncidentAndMachine): Boolean {
            return oldItem.incident.id == newItem.incident.id
        }

        override fun areContentsTheSame(oldItem: IncidentAndMachine, newItem: IncidentAndMachine): Boolean {
            return oldItem == newItem
        }
    }
}
