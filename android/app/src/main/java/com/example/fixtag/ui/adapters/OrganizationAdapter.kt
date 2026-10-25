package com.example.fixtag.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.R
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.SyncStatus

class OrganizationAdapter(
    private val onDelete: ((Organization) -> Unit)? = null,
) : ListAdapter<Organization, OrganizationAdapter.ViewHolder>(OrganizationDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_organization, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.name)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(organization: Organization) {
            nameTextView.text = organization.name
            deleteButton.isVisible = onDelete != null && organization.syncStatus == SyncStatus.Pending
            deleteButton.setOnClickListener { onDelete?.invoke(organization) }
        }
    }

    // DiffUtil handles smart list updates completely automatically
    object OrganizationDiffCallback : DiffUtil.ItemCallback<Organization>() {
        override fun areItemsTheSame(oldItem: Organization, newItem: Organization): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Organization, newItem: Organization): Boolean {
            return oldItem == newItem
        }
    }
}