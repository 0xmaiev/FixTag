package com.example.fixtag.ui.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.R
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.MachineWithStats
import com.example.fixtag.data.model.SyncStatus
import com.example.fixtag.ui.MACHINE_ID_KEY
import com.example.fixtag.ui.recentIncidentsText

class MachineAdapter(
    private val onDelete: ((Machine) -> Unit)? = null,
) : ListAdapter<MachineWithStats, MachineAdapter.ViewHolder>(UserDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_machine, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val modelTextView: TextView = itemView.findViewById(R.id.model)
        private val serialNumberTextView: TextView = itemView.findViewById(R.id.serialNumber)
        private val statusTextView: TextView = itemView.findViewById(R.id.machineStatus)
        private val historyTextView: TextView = itemView.findViewById(R.id.description)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(item: MachineWithStats) {
            val machine = item.machine
            val context = itemView.context
            modelTextView.text = machine.model
            serialNumberTextView.text = machine.serialNumber

            // green with nothing open, yellow with open reports, red while one of them is critical
            val (background, textColor) = when {
                item.openIncidents == 0 -> android.R.color.holo_green_dark to R.color.white
                item.criticalIncidents > 0 -> R.color.red to R.color.white
                else -> R.color.yellow to R.color.black
            }
            statusTextView.text = if (item.openIncidents == 0) context.getString(R.string.machine_ok)
            else context.getString(R.string.machine_open_incidents, item.openIncidents)
            statusTextView.setBackgroundColor(ContextCompat.getColor(context, background))
            statusTextView.setTextColor(ContextCompat.getColor(context, textColor))
            historyTextView.text = context.recentIncidentsText(item.recentIncidents)

            deleteButton.isVisible = onDelete != null && machine.syncStatus == SyncStatus.Pending
            deleteButton.setOnClickListener { onDelete?.invoke(machine) }
            itemView.setOnClickListener {
                findNavController(itemView).navigate(
                    R.id.machine_details, bundleOf(MACHINE_ID_KEY to machine.id.toString()))
            }
        }
    }

    // DiffUtil handles smart list updates completely automatically
    object UserDiffCallback : DiffUtil.ItemCallback<MachineWithStats>() {
        override fun areItemsTheSame(oldItem: MachineWithStats, newItem: MachineWithStats): Boolean {
            return oldItem.machine.id == newItem.machine.id
        }

        override fun areContentsTheSame(oldItem: MachineWithStats, newItem: MachineWithStats): Boolean {
            return oldItem == newItem
        }
    }
}
