package com.example.fixtag.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.R
import com.example.fixtag.data.model.Organization

class OrganizationSelectAdapter(
    onItemSelected: (Organization?) -> Unit
) : BaseSelectAdapter<Organization, OrganizationSelectAdapter.ViewHolder>(
    OrganizationDiffCallback(),
    itemIdProvider = { it.id }, // Tell the base class how to get the ID
    onItemSelected = onItemSelected
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_organization, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val organization = getItem(position)
        // `isItemSelected` comes from the base class
        holder.bind(organization, isItemSelected(organization))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mainLinearLayout: LinearLayout = itemView.findViewById(R.id.mainLinearLayout)
        private val nameTextView: TextView = itemView.findViewById(R.id.name)

        fun bind(organization: Organization, isSelected: Boolean) {
            nameTextView.text = organization.name

            val context = itemView.context
            val colorRes = if (isSelected) R.color.grey_blue_selected else R.color.grey_blue
            mainLinearLayout.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, colorRes))

            itemView.setOnClickListener {
                // Route the click event through the base class logic
                handleItemClick(organization)
            }
        }
    }
}

// Standard DiffUtil for performance optimization
class OrganizationDiffCallback : DiffUtil.ItemCallback<Organization>() {
    override fun areItemsTheSame(oldItem: Organization, newItem: Organization) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Organization, newItem: Organization) = oldItem == newItem
}