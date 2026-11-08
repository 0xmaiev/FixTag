package com.example.fixtag.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.R
import com.example.fixtag.data.model.User
import com.example.fixtag.ui.label

class MemberAdapter : ListAdapter<User, MemberAdapter.ViewHolder>(MemberDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.name)
        private val emailTextView: TextView = itemView.findViewById(R.id.email)
        private val roleTextView: TextView = itemView.findViewById(R.id.role)

        fun bind(user: User) {
            nameTextView.text = user.name
            emailTextView.text = user.email
            roleTextView.setText(user.role.label())
        }
    }

    object MemberDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
}
