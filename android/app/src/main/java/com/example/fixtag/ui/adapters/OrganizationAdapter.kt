package com.example.fixtag.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.R
import com.example.fixtag.data.model.Organization

class OrganizationAdapter : ListAdapter<Organization, OrganizationAdapter.ViewHolder>(OrganizationDiffCallback) {

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

        fun bind(organization: Organization) {
            nameTextView.text = organization.name
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