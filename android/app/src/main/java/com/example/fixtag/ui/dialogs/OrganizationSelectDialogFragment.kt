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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.data.model.Organization
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.ORGANIZATION_ID_KEY
import com.example.fixtag.ui.SELECT_ORGANIZATION_REQUEST_KEY
import com.example.fixtag.ui.adapters.OrganizationSelectAdapter
import com.example.fixtag.ui.viewmodels.OrganizationsViewModel
import kotlinx.coroutines.launch

// A dialog-based list with single item selection.
class OrganizationSelectDialogFragment(
    private val initialSelection: Organization?
) : DialogFragment() {

    companion object {
        fun newInstance() = OrganizationSelectDialogFragment
    }

    private val viewModel: OrganizationsViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        AppViewModelFactory(repository)
    }

    private lateinit var adapter: OrganizationSelectAdapter
    private lateinit var select: Button
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState ?: Bundle())

        adapter = OrganizationSelectAdapter { organization ->
            val bundle = Bundle()
            bundle.putString(ORGANIZATION_ID_KEY, organization?.id.toString())
            setFragmentResult(SELECT_ORGANIZATION_REQUEST_KEY, bundle)
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
        return inflater.inflate(R.layout.dialog_organization_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        select = view.findViewById(R.id.select)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { organizations ->
                    // submit data from vm flow to recycler view adapter for display
                    adapter.submitList(organizations)
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