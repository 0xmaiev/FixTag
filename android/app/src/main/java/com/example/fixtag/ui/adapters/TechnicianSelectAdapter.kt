package com.example.fixtag.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.R
import com.example.fixtag.data.model.Technician

class TechnicianSelectAdapter(
    onItemSelected: (Technician?) -> Unit
) : BaseSelectAdapter<Technician, TechnicianSelectAdapter.ViewHolder>(
    TechnicianDiffCallback(),
    itemIdProvider = { it.id }, // Tell the base class how to get the ID
    onItemSelected = onItemSelected
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_technician, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val technician = getItem(position)
        // `isItemSelected` comes from the base class
        holder.bind(technician, isItemSelected(technician))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mainLinearLayout: LinearLayout = itemView.findViewById(R.id.mainLinearLayout)
        private val nameTextView: TextView = itemView.findViewById(R.id.name)
        private val openIncidentsTextView: TextView = itemView.findViewById(R.id.openIncidents)

        fun bind(technician: Technician, isSelected: Boolean) {
            nameTextView.text = technician.name
            openIncidentsTextView.text = technician.openIncidents.toString()

            // Route the click event through the base class logic

            val context = itemView.context
            val colorRes = if (isSelected) R.color.grey_blue_selected else R.color.grey_blue
            mainLinearLayout.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, colorRes))

            itemView.setOnClickListener {
                // Route the click event through the base class logic
                handleItemClick(technician)
            }
        }
    }

}

// Standard DiffUtil for performance optimization
class TechnicianDiffCallback : DiffUtil.ItemCallback<Technician>() {
    override fun areItemsTheSame(oldItem: Technician, newItem: Technician) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Technician, newItem: Technician) = oldItem == newItem
}