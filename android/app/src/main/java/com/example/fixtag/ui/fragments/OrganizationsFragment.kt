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
import com.example.fixtag.data.model.UserRole
import com.example.fixtag.ui.adapters.OrganizationAdapter
import com.example.fixtag.ui.confirmDeletePending
import com.example.fixtag.ui.main.MainViewModel
import com.example.fixtag.ui.main.MainViewModelFactory
import com.example.fixtag.ui.showFor
import com.example.fixtag.ui.showPendingCount
import com.example.fixtag.ui.viewmodels.OrganizationsViewModel
import kotlinx.coroutines.launch

class OrganizationsFragment : Fragment() {

    companion object {
        fun newInstance() = OrganizationsFragment()
    }

    private val mainViewModel: MainViewModel by activityViewModels {
        val app = requireContext().applicationContext as FixTag
        MainViewModelFactory(app.repository, app.syncScheduler)
    }

    private val viewModel: OrganizationsViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        AppViewModelFactory(repository)
    }

    private lateinit var switchProfileButton: Button
    private lateinit var organizationAdapter: OrganizationAdapter
    private lateinit var newOrganization: Button
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        organizationAdapter = OrganizationAdapter(onDelete = { organization ->
            confirmDeletePending(organization.name, R.string.delete_organization_in_use) {
                viewModel.delete(organization)
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_organizations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchProfileButton = view.findViewById(R.id.switchProfileButton)
        newOrganization = view.findViewById(R.id.newOrganizationButton)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.adapter = organizationAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { organizations ->
                    organizationAdapter.submitList(organizations)
                }
            }
        }
        newOrganization.setOnClickListener {
            findNavController().navigate(R.id.new_organization)
        }
        switchProfileButton.setOnClickListener {
            mainViewModel.switchProfile()
        }
        showPendingCount(view, mainViewModel.pendingCount)
        showFor(newOrganization, mainViewModel.role, UserRole.Assistance)
    }

}