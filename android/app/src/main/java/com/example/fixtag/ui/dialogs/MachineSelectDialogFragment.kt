package com.example.fixtag.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.data.model.Machine
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.MACHINE_ID_KEY
import com.example.fixtag.ui.SELECT_MACHINE_REQUEST_KEY
import com.example.fixtag.ui.adapters.MachineSelectAdapter
import com.example.fixtag.ui.viewmodels.MachinesViewModel
import kotlinx.coroutines.launch

// A dialog-based list with single item selection.
class MachineSelectDialogFragment(
    private val initialSelection: Machine?
) : DialogFragment() {

    companion object {
        fun newInstance() = MachineSelectDialogFragment
    }

    private val viewModel: MachinesViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        AppViewModelFactory(repository)
    }

    private lateinit var adapter: MachineSelectAdapter
    private lateinit var select: Button
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState ?: Bundle())

        adapter = MachineSelectAdapter { machine ->
            val bundle = Bundle()
            bundle.putString(MACHINE_ID_KEY, machine?.id.toString())
            setFragmentResult(SELECT_MACHINE_REQUEST_KEY, bundle)
        }
        adapter.setInitialSelection(initialSelection?.id)

        // allow canceling by pressing back button
        setCancelable(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_machine_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        select = view.findViewById(R.id.select)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { machines ->
                    adapter.submitList(machines)
                }
            }
        }
        select.setOnClickListener {
            // just need to dismiss dialog because we were propagating result
            // via `setFragmentResult` above
            dismiss()
        }
    }

}