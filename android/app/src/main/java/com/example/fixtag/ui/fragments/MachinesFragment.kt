package com.example.fixtag.ui.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.ui.adapters.MachineAdapter
import com.example.fixtag.ui.confirmDeletePending
import com.example.fixtag.ui.main.MainViewModel
import com.example.fixtag.ui.main.MainViewModelFactory
import com.example.fixtag.ui.showPendingCount
import com.example.fixtag.ui.viewmodels.MachinesViewModel
import kotlinx.coroutines.launch

class MachinesFragment : Fragment() {

    companion object {
        fun newInstance() = MachinesFragment()
    }

    private val mainViewModel: MainViewModel by activityViewModels {
        val app = requireContext().applicationContext as FixTag
        MainViewModelFactory(app.repository, app.syncScheduler)
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

        machineAdapter = MachineAdapter(onDelete = { machine ->
            confirmDeletePending(machine.model, R.string.delete_machine_in_use) {
                viewModel.delete(machine)
            }
        })
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
        showPendingCount(view, mainViewModel.pendingCount)
    }

}