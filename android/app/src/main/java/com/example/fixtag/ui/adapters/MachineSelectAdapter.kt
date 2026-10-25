package com.example.fixtag.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.R
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.MachineWithStats

class MachineSelectAdapter(
    onItemSelected: (MachineWithStats?) -> Unit
) : BaseSelectAdapter<MachineWithStats, MachineSelectAdapter.ViewHolder>(
    MachineDiffCallback(),
    itemIdProvider = { it.machine.id }, // Tell the base class how to get the ID
    onItemSelected = onItemSelected
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_machine, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val machine = getItem(position)
        // `isItemSelected` comes from the base class
        holder.bind(machine, isItemSelected(machine))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mainLinearLayout: LinearLayout = itemView.findViewById(R.id.mainLinearLayout)
        private val modelTextView: TextView = itemView.findViewById(R.id.model)
        private val serialNumberTextView: TextView = itemView.findViewById(R.id.serialNumber)

        fun bind(data: MachineWithStats, isSelected: Boolean) {
            modelTextView.text = data.machine.model
            serialNumberTextView.text = data.machine.serialNumber

            val context = itemView.context
            val colorRes = if (isSelected) R.color.grey_blue_selected else R.color.grey_blue
            mainLinearLayout.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, colorRes))

            itemView.setOnClickListener {
                // Route the click event through the base class logic
                handleItemClick(data)
            }
        }
    }
}

// Standard DiffUtil for performance optimization
class MachineDiffCallback : DiffUtil.ItemCallback<MachineWithStats>() {
    override fun areItemsTheSame(oldItem: MachineWithStats, newItem: MachineWithStats) = oldItem.machine.id == newItem.machine.id
    override fun areContentsTheSame(oldItem: MachineWithStats, newItem: MachineWithStats) = oldItem == newItem
}