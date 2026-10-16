package com.example.fixtag.ui.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.MACHINE_ID_KEY
import com.example.fixtag.ui.adapters.IncidentAdapter
import com.example.fixtag.ui.viewmodels.MachineDetailViewModel
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.uuid.Uuid

class MachineDetailFragment : Fragment() {

    companion object {
        fun newInstance() = MachineDetailFragment()
    }

    private val viewModel: MachineDetailViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        ItemViewModelFactory(repository)
    }

    private lateinit var machineId: Uuid
    private lateinit var incidentAdapter: IncidentAdapter
    private lateinit var backButton: Button
    private lateinit var newIncidentButton: Button
    private lateinit var machineModel: TextView
    private lateinit var machineSerialNumber: TextView
    private lateinit var machineHistorySummary: TextView
    private lateinit var incidentsSectionLayout: LinearLayout
    private lateinit var incidentsRecyclerView: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        incidentAdapter = IncidentAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_machine_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backButton = view.findViewById(R.id.backButton)
        newIncidentButton = view.findViewById(R.id.newIncidentButton)
        machineModel = view.findViewById(R.id.model)
        machineSerialNumber = view.findViewById(R.id.serialNumber)
        machineHistorySummary = view.findViewById(R.id.machineHistorySummary)
        incidentsSectionLayout = view.findViewById(R.id.incidentsSectionLinearLayout)
        incidentsRecyclerView = view.findViewById(R.id.incidentsRecyclerView)
        incidentsRecyclerView.adapter = incidentAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.incidents.collect { data ->
                        val now = Clock.System.now()
                        val incidentsLastMonth = data.count {
                            item -> (now - item.createdAt).toInt(DurationUnit.SECONDS) < 30 * 24 * 60 * 60
                        }
                        machineHistorySummary.text = resources.getQuantityString(R.plurals.machine_history_summary, incidentsLastMonth)
                    }
                }
                launch {
                    viewModel.machine.collect { machine ->
                        if (machine != null) {
                            machineId = machine.id
                            machineModel.text = machine.model
                            machineSerialNumber.text = machine.serialNumber
                        }
                    }
                }
            }
        }

        backButton.setOnClickListener {
            findNavController(view).navigateUp()
        }

        newIncidentButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString(MACHINE_ID_KEY, machineId.toString())
            findNavController().navigate(
                R.id.new_incident,
                bundle
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}