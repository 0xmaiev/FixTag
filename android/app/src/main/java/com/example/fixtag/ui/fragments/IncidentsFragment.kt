package com.example.fixtag.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.data.model.SyncStatus
import kotlinx.coroutines.launch
import kotlin.time.Clock


class IncidentsAdapter : ListAdapter<Incident, IncidentsAdapter.ViewHolder>(UserDiffCallback) {

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
        private val descriptionTextView: TextView = itemView.findViewById(R.id.description)
        private val statusTextView: TextView = itemView.findViewById(R.id.status)
        private val syncStatusTextView: TextView = itemView.findViewById(R.id.syncStatus)

        fun bind(incident: Incident) {
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
                    val elapsed = Clock.System.now() - incident.updatedAt
                    // TODO: change based on time elapsed, few mins ago, hours days etc
                    elapsed.inWholeHours.toString()
                }
            }
            syncStatusTextView.text = syncStatus

            itemView.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("id", incident.id.toString())
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

class IncidentsFragment : Fragment() {

    companion object {
        fun newInstance() = IncidentsFragment()
    }

    private val viewModel: IncidentsViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        AppViewModelFactory(repository)
    }

    private lateinit var incidentsAdapter: IncidentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_incidents, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Set up the RecyclerView
        incidentsAdapter = IncidentsAdapter()
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.adapter = incidentsAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { incidents ->
                    incidentsAdapter.submitList(incidents)
                }
            }
        }
    }

}