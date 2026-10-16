package com.example.fixtag.ui.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.R
import com.example.fixtag.data.model.Machine
import com.example.fixtag.ui.MACHINE_ID_KEY


class MachineAdapter : ListAdapter<Machine, MachineAdapter.ViewHolder>(UserDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_machine, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val modelTextView: TextView = itemView.findViewById(R.id.model)
        private val serialNumberTextView: TextView = itemView.findViewById(R.id.serialNumber)

        fun bind(machine: Machine) {
            modelTextView.text = machine.model
            serialNumberTextView.text = machine.serialNumber

            itemView.setOnClickListener {
                val bundle = Bundle()
                bundle.putString(MACHINE_ID_KEY, machine.id.toString())
                findNavController(itemView).navigate(
                    R.id.machine_details,
                    bundle
                )
            }
        }
    }

    // DiffUtil handles smart list updates completely automatically
    object UserDiffCallback : DiffUtil.ItemCallback<Machine>() {
        override fun areItemsTheSame(oldItem: Machine, newItem: Machine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Machine, newItem: Machine): Boolean {
            return oldItem == newItem
        }
    }
}
