package com.example.fixtag.ui.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.format.DateUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.FixTag
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.R
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.IncidentUpdate
import com.example.fixtag.ui.viewmodels.IncidentDetailViewModel
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid


class MessageAdapter : ListAdapter<IncidentUpdate, MessageAdapter.ViewHolder>(UserDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_update, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val authorNameTextView: TextView = itemView.findViewById(R.id.authorName)
        private val authorRoleTextView: TextView = itemView.findViewById(R.id.authorRole)
        private val timeElapsedTextView: TextView = itemView.findViewById(R.id.timeElapsed)
        private val messageTextView: TextView = itemView.findViewById(R.id.message)

        fun bind(incidentUpdate: IncidentUpdate) {
            authorNameTextView.text = incidentUpdate.authorName
            val elapsed = DateUtils.getRelativeTimeSpanString( incidentUpdate.createdAt.toEpochMilliseconds())
            timeElapsedTextView.text = elapsed
        }
    }

    // DiffUtil handles smart list updates completely automatically
    object UserDiffCallback : DiffUtil.ItemCallback<IncidentUpdate>() {
        override fun areItemsTheSame(oldItem: IncidentUpdate, newItem: IncidentUpdate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IncidentUpdate, newItem: IncidentUpdate): Boolean {
            return oldItem == newItem
        }
    }
}

class IncidentDetailFragment : Fragment() {

    companion object {
        fun newInstance() = IncidentDetailFragment()
    }

    private lateinit var incidentId: Uuid
    private lateinit var backButton: Button
    private lateinit var incidentDescription: TextView
    private lateinit var incidentSeverity: TextView
    private lateinit var machineModel: TextView
    private lateinit var machineSerialNumber: TextView
    private lateinit var machineHistorySummary: TextView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messagesSectionLayout: LinearLayout
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var photosRecyclerView: RecyclerView

    private val viewModel: IncidentDetailViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        ItemViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        messageAdapter = MessageAdapter()

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_incident_detail, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backButton = view.findViewById(R.id.backButton)
        incidentSeverity = view.findViewById(R.id.incidentSeverity)
        incidentDescription = view.findViewById(R.id.incidentDescription)
        machineModel = view.findViewById(R.id.machineModel)
        machineSerialNumber = view.findViewById(R.id.machineSerialNumber)
        machineHistorySummary = view.findViewById(R.id.machineHistorySummary)
        messagesSectionLayout = view.findViewById(R.id.messagesSectionLinearLayout)
        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView)
        messagesRecyclerView.adapter = messageAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.incident.collect { data ->
                        if (data != null) {
                            incidentDescription.text = data.incident.description
                            incidentSeverity.text = data.incident.severity.toString()
                            val severityBackgroundColor = when(data.incident.severity) {
                                IncidentSeverity.Low -> {
                                    R.color.grey_blue
                                }
                                IncidentSeverity.Medium -> {
                                    R.color.yellow
                                }
                                IncidentSeverity.Critical -> {
                                    R.color.red
                                }
                            }
                            incidentSeverity.setBackgroundColor(severityBackgroundColor)
                            machineModel.text = data.machine.model
                            machineSerialNumber.text = data.machine.serialNumber
                        }
                    }
                }
                launch {
                    viewModel.updates.collect { updates ->
                        messagesSectionLayout.isVisible = updates.isNotEmpty()
                        messageAdapter.submitList(updates)
                    }
                }
            }
        }

        backButton.setOnClickListener {
            findNavController(view).navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

}