package com.example.fixtag.ui.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.FixTag
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.R
import com.example.fixtag.data.model.AttachmentKind
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.ui.INCIDENT_ID_KEY
import com.example.fixtag.ui.VoicePlayer
import com.example.fixtag.ui.adapters.AttachmentAdapter
import com.example.fixtag.ui.adapters.MessageAdapter
import com.example.fixtag.ui.label
import com.example.fixtag.ui.showPhoto
import com.example.fixtag.ui.showSeverity
import com.example.fixtag.ui.viewmodels.IncidentDetailViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class IncidentDetailFragment : Fragment() {

    companion object {
        fun newInstance() = IncidentDetailFragment()
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var incidentDescription: TextView
    private lateinit var incidentSeverity: TextView
    private lateinit var machineModel: TextView
    private lateinit var machineSerialNumber: TextView
    private lateinit var machineHistorySummary: TextView
    private lateinit var technician: TextView
    private lateinit var selectTechnician: Button
    private lateinit var addUpdate: Chip
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var attachmentAdapter: AttachmentAdapter
    private lateinit var messagesHeader: TextView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var attachmentsRecyclerView: RecyclerView
    private val voicePlayer = VoicePlayer()

    private val viewModel: IncidentDetailViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        ItemViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        messageAdapter = MessageAdapter()
        attachmentAdapter = AttachmentAdapter(onOpen = { item ->
            when (item.kind) {
                AttachmentKind.Photo -> showPhoto(item.path!!)
                AttachmentKind.Audio -> voicePlayer.toggle(item.path!!)
                AttachmentKind.Video -> Unit
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_incident_detail, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        toolbar.inflateMenu(R.menu.menu_incident_detail)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId != R.id.edit) return@setOnMenuItemClickListener false
            // the same form as a new report, opened on this incident
            val bundle = Bundle()
            bundle.putString(INCIDENT_ID_KEY, viewModel.id.toString())
            findNavController().navigate(R.id.new_incident, bundle)
            true
        }
        incidentSeverity = view.findViewById(R.id.incidentSeverity)
        incidentDescription = view.findViewById(R.id.incidentDescription)
        machineModel = view.findViewById(R.id.machineModel)
        machineSerialNumber = view.findViewById(R.id.machineSerialNumber)
        machineHistorySummary = view.findViewById(R.id.machineHistorySummary)
        technician = view.findViewById(R.id.technician)
        selectTechnician = view.findViewById(R.id.selectTechnicianButton)
        addUpdate = view.findViewById(R.id.chip)
        messagesHeader = view.findViewById(R.id.messagesSectionHeaderTextView)
        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView)
        messagesRecyclerView.adapter = messageAdapter
        attachmentsRecyclerView = view.findViewById(R.id.attachments)
        attachmentsRecyclerView.adapter = attachmentAdapter

        selectTechnician.setOnClickListener { pickTechnician() }
        addUpdate.setOnClickListener { showUpdateDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.incident.collect { data ->
                        if (data != null) {
                            toolbar.subtitle = getString(data.incident.status.label())
                            incidentDescription.text = data.incident.description
                            incidentSeverity.text = data.incident.severity.toString()
                            incidentSeverity.showSeverity(data.incident.severity)
                            machineModel.text = data.machine.model
                            machineSerialNumber.text = data.machine.serialNumber
                            technician.text = data.incident.assignedToName
                                ?: getString(R.string.no_technician_assigned)
                        }
                    }
                }
                launch {
                    viewModel.permissions.collect { allowed ->
                        toolbar.menu.findItem(R.id.edit).isVisible = allowed.edit
                        selectTechnician.isVisible = allowed.assign
                    }
                }
                launch {
                    viewModel.updates.collect { updates ->
                        // only the header: the section also holds the button for the first update
                        messagesHeader.isVisible = updates.isNotEmpty()
                        messageAdapter.submitList(updates)
                    }
                }
                launch {
                    viewModel.attachments.collect { attachmentAdapter.submitList(it) }
                }
                launch {
                    voicePlayer.playing.collect { attachmentAdapter.playingPath = it }
                }
            }
        }
    }

    // support staff hand the incident to one of their technicians, the open count helps spread them
    private fun pickTechnician() {
        viewLifecycleOwner.lifecycleScope.launch {
            val technicians = viewModel.technicians()
            val current = viewModel.incident.value?.incident?.assignedTo
            val labels = technicians.map {
                val open = resources.getQuantityString(
                    R.plurals.technician_open_incidents, it.openIncidents, it.openIncidents)
                "${it.user.name} · $open"
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.technician_select_dialog_title)
                .setSingleChoiceItems(labels.toTypedArray(),
                    technicians.indexOfFirst { it.user.id == current }) { dialog, which ->
                    viewModel.assign(technicians[which].user)
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun showUpdateDialog() {
        val form = layoutInflater.inflate(R.layout.dialog_incident_update, null)
        val message = form.findViewById<EditText>(R.id.message)
        val status = form.findViewById<ChipGroup>(R.id.status)
        status.isVisible = viewModel.permissions.value.changeStatus
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_incident_update)
            .setView(form)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)  // replaced below, so an error keeps the dialog open
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newStatus = when (status.checkedChipId) {
                R.id.statusAnalysisChip -> IncidentStatus.Analysis
                R.id.statusResolvedChip -> IncidentStatus.Resolved
                else -> null
            }
            if (viewModel.postUpdate(message.text.toString(), newStatus)) {
                Toast.makeText(requireContext(), R.string.update_saved, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                message.error = getString(R.string.update_empty_error)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        voicePlayer.stop()
    }
}
