package com.example.fixtag.ui.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.R
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.data.model.SyncStatus
import com.example.fixtag.ui.INCIDENT_ID_KEY


class IncidentAdapter : ListAdapter<Incident, IncidentAdapter.ViewHolder>(UserDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_incident, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // TODO: need to attach machine data too
        private val modelTextView: TextView = itemView.findViewById(R.id.model)
        private val serialNumberTextView: TextView = itemView.findViewById(R.id.serialNumber)
        private val severityTextView: TextView = itemView.findViewById(R.id.severity)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.description)
        private val statusTextView: TextView = itemView.findViewById(R.id.status)
        private val syncStatusTextView: TextView = itemView.findViewById(R.id.syncStatus)

        fun bind(incident: Incident) {
            val severityBackgroundColor = when(incident.severity) {
                IncidentSeverity.Low -> {
                    R.color.grey_blue
                }
                IncidentSeverity.Medium -> {
                    R.color.yellow
                }
                IncidentSeverity.Critical -> {
                    R.color.red
                }
            }
            severityTextView.text = incident.severity.toString()
            severityTextView.setBackgroundColor(severityBackgroundColor)
            descriptionTextView.text = incident.description
            val status = when(incident.syncStatus) {
                SyncStatus.Pending ->  itemView.context.getString(R.string.pending_sync)
                else -> {
                    when(incident.status) {
                        IncidentStatus.Received -> itemView.context.getString(R.string.incident_received)
                        IncidentStatus.Analysis -> itemView.context.getString(R.string.incident_analysis)
                        IncidentStatus.Resolved -> itemView.context.getString(R.string.incident_resolved)
                    }
                }
            }
            statusTextView.text = status
            val syncStatus = when(incident.syncStatus) {
                SyncStatus.Pending ->  itemView.context.getString(R.string.stored_locally)
                else -> {
                    val elapsed = DateUtils.getRelativeTimeSpanString( incident.createdAt.toEpochMilliseconds())
                    elapsed
                }
            }
            syncStatusTextView.text = syncStatus

            itemView.setOnClickListener {
                val bundle = Bundle()
                bundle.putString(INCIDENT_ID_KEY, incident.id.toString())
                findNavController(itemView).navigate(
                    R.id.incident_details,
                    bundle
                )
            }
        }
    }

    // DiffUtil handles smart list updates completely automatically
    object UserDiffCallback : DiffUtil.ItemCallback<Incident>() {
        override fun areItemsTheSame(oldItem: Incident, newItem: Incident): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Incident, newItem: Incident): Boolean {
            return oldItem == newItem
        }
    }
}
