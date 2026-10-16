package com.example.fixtag.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.MACHINE_ID_KEY
import com.example.fixtag.ui.SELECT_MACHINE_REQUEST_KEY
import com.example.fixtag.ui.adapters.MachineSelectAdapter
import com.example.fixtag.ui.viewmodels.MachinesViewModel
import kotlinx.coroutines.launch

// A dialog-based list with single item selection.
class MachineSelectDialogFragment : DialogFragment() {

    companion object {
        fun newInstance() = MachineSelectDialogFragment
    }

    private val viewModel: MachinesViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        AppViewModelFactory(repository)
    }

    private lateinit var adapter: MachineSelectAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState ?: Bundle())

        adapter = MachineSelectAdapter { organization ->
            if (organization != null) {
                val bundle = Bundle()
                bundle.putString(MACHINE_ID_KEY, organization.id.toString())
                setFragmentResult(SELECT_MACHINE_REQUEST_KEY, bundle)
            } else {
                // since we are only returning one value we can clear the whole result as a stopgap
                clearFragmentResult(MACHINE_ID_KEY)
            }
        }

        // allow canceling by pressing back button
        setCancelable(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_organization_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { organizations ->
                    adapter.submitList(organizations)
                }
            }
        }
    }

}