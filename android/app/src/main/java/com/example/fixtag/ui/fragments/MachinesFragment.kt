package com.example.fixtag.ui.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.data.model.Machine
import kotlinx.coroutines.launch

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
                bundle.putString("id", machine.id.toString())
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

class MachinesFragment : Fragment() {

    companion object {
        fun newInstance() = MachinesFragment()
    }

    private val viewModel: MachinesViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        AppViewModelFactory(repository)
    }

    private lateinit var machineAdapter: MachineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_machines, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Set up the RecyclerView
        machineAdapter = MachineAdapter()
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.adapter = machineAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.machines.collect { machines ->
                    machineAdapter.submitList(machines)
                }
            }
        }
    }

}