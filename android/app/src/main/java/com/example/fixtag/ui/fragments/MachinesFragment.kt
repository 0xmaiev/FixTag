package com.example.fixtag.ui.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.ui.adapters.MachineAdapter
import com.example.fixtag.ui.viewmodels.MachinesViewModel
import kotlinx.coroutines.launch

class MachinesFragment : Fragment() {

    companion object {
        fun newInstance() = MachinesFragment()
    }

    private val viewModel: MachinesViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        AppViewModelFactory(repository)
    }


    private lateinit var newMachineButton: Button
    private lateinit var machineAdapter: MachineAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        machineAdapter = MachineAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_machines, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        newMachineButton = view.findViewById(R.id.newMachineButton)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = machineAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { machines ->
                    machineAdapter.submitList(machines)
                }
            }
        }
        newMachineButton.setOnClickListener {
            findNavController().navigate(R.id.new_machine)
        }
    }

}