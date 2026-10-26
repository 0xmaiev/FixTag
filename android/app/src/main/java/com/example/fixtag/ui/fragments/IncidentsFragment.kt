package com.example.fixtag.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.ui.adapters.IncidentAdapter
import com.example.fixtag.ui.confirmDeletePending
import com.example.fixtag.ui.main.MainViewModel
import com.example.fixtag.ui.main.MainViewModelFactory
import com.example.fixtag.ui.showPendingCount
import com.example.fixtag.ui.viewmodels.IncidentsViewModel
import kotlinx.coroutines.launch


class IncidentsFragment : Fragment() {

    companion object {
        fun newInstance() = IncidentsFragment()
    }

    private val mainViewModel: MainViewModel by activityViewModels {
        val app = requireContext().applicationContext as FixTag
        MainViewModelFactory(app.repository, app.syncScheduler)
    }

    private val viewModel: IncidentsViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        AppViewModelFactory(repository)
    }

    private lateinit var newIncidentButton: Button
    private lateinit var switchProfileButton: Button
    private lateinit var incidentAdapter: IncidentAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        incidentAdapter = IncidentAdapter(onDelete = { incident ->
            confirmDeletePending(incident.description, R.string.delete_pending_message) {
                viewModel.delete(incident)
            }
        })
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

        newIncidentButton = view.findViewById(R.id.newIncidentButton)
        switchProfileButton = view.findViewById(R.id.switchProfileButton)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = incidentAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { incidents ->
                    incidentAdapter.submitList(incidents)
                }
            }
        }
        newIncidentButton.setOnClickListener {
            findNavController().navigate(R.id.new_incident)
        }
        switchProfileButton.setOnClickListener {
            mainViewModel.switchProfile()
        }
        showPendingCount(view, mainViewModel.pendingCount)
    }

}