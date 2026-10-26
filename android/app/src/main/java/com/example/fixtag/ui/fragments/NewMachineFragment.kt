package com.example.fixtag.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.Organization
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.ORGANIZATION_ID_KEY
import com.example.fixtag.ui.SELECT_ORGANIZATION_REQUEST_KEY
import com.example.fixtag.ui.dialogs.OrganizationSelectDialogFragment
import com.example.fixtag.ui.viewmodels.InputBaseViewModel
import com.example.fixtag.ui.viewmodels.NewMachineViewModel
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class NewMachineFragment: Fragment() {

    companion object {
        fun newInstance() = NewIncidentFragment()
    }

    private val viewModel: NewMachineViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        ItemViewModelFactory(repository)
    }

    private lateinit var selectOrganization: Button
    private lateinit var createMachine: Button
    private lateinit var model: TextView
    private lateinit var serialNumber: TextView
    private lateinit var organization: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_machine, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { findNavController().navigateUp() }
        selectOrganization = view.findViewById(R.id.selectOrganizationButton)
        createMachine = view.findViewById(R.id.createMachineButton)
        model = view.findViewById(R.id.model)
        serialNumber = view.findViewById(R.id.serialNumber)
        organization = view.findViewById(R.id.organization)

        // select associated organization
        selectOrganization.setOnClickListener {
            // shown as this fragment's child, so the result comes back here
            OrganizationSelectDialogFragment(viewModel.organization.value).show(childFragmentManager,
                SELECT_ORGANIZATION_REQUEST_KEY
            )
        }
        // create the machine
        createMachine.setOnClickListener {
            viewModel.submit(model.text.toString(), serialNumber.text.toString())
        }
        // set listener for result from ´OrganizationSelectDialogFragment`
        childFragmentManager.setFragmentResultListener(
            SELECT_ORGANIZATION_REQUEST_KEY,
            viewLifecycleOwner) { _, result ->
            result.getString(ORGANIZATION_ID_KEY)?.let {
                viewModel.selectOrganization(it)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.form.collect { form ->
                        createMachine.isEnabled = form.isDataValid
                        organization.error = form.organizationError?.let { getString(it) }
                        model.error = form.modelError?.let { getString(it) }
                        serialNumber.error = form.serialNumberError?.let { getString(it) }
                    }
                }
                launch {
                    viewModel.organization.collect {
                        value -> organization.text = value?.name ?: getString(R.string.no_organization_selected)
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        if (state == InputBaseViewModel.State.Saved) {
                            Toast.makeText(
                                requireContext(),
                                R.string.machine_saved,
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}