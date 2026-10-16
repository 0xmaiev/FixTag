package com.example.fixtag.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
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
import com.example.fixtag.ui.viewmodels.IncidentsViewModel
import kotlinx.coroutines.launch


class IncidentsFragment : Fragment() {

    companion object {
        fun newInstance() = IncidentsFragment()
    }

    private val viewModel: IncidentsViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        AppViewModelFactory(repository)
    }

    private lateinit var newIncidentButton: Button
    private lateinit var incidentAdapter: IncidentAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        incidentAdapter = IncidentAdapter()
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
    }

}