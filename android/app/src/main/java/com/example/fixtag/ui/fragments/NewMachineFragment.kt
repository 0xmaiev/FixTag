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
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.Organization
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.ORGANIZATION_ID_KEY
import com.example.fixtag.ui.SELECT_ORGANIZATION_REQUEST_KEY
import com.example.fixtag.ui.dialogs.OrganizationSelectDialogFragment
import com.example.fixtag.ui.viewmodels.NewMachineViewModel

class NewMachineFragment: Fragment() {

    companion object {
        fun newInstance() = NewIncidentFragment()
    }

    private val viewModel: NewMachineViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        ItemViewModelFactory(repository)
    }

    private lateinit var back: Button
    private lateinit var selectOrganization: Button
    private lateinit var createMachine: Button
    private lateinit var loading: ProgressBar
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

        back = view.findViewById(R.id.backButton)
        selectOrganization = view.findViewById(R.id.selectOrganizationButton)
        createMachine = view.findViewById(R.id.createMachineButton)
        loading = view.findViewById(R.id.loading)
        model = view.findViewById(R.id.model)
        serialNumber = view.findViewById(R.id.serialNumber)
        organization = view.findViewById(R.id.organization)

        // navigate back
        back.setOnClickListener { findNavController().navigateUp() }
        // select associated organization
        selectOrganization.setOnClickListener {
            // shown as this fragment's child, so the result comes back here
            OrganizationSelectDialogFragment().show(childFragmentManager,
                SELECT_ORGANIZATION_REQUEST_KEY
            )
        }
        // create the machine
        createMachine.setOnClickListener {
            viewModel.submit()
        }
        // set listener for result from ´OrganizationSelectDialogFragment`
        childFragmentManager.setFragmentResultListener(
            SELECT_ORGANIZATION_REQUEST_KEY,
            viewLifecycleOwner) { _, result ->
            result.getString(ORGANIZATION_ID_KEY)?.let {
                viewModel.formDataChanged(
                    it,
                    model.text.toString(),
                    serialNumber.text.toString()
                )
            }
        }
        // react to form validation
        viewModel.form.observe(viewLifecycleOwner, Observer {
            val state = it ?: return@Observer

            // disable login button unless both username / password is valid
            createMachine.isEnabled = state.isDataValid

            if (state.modelError != null) {
                model.error = getString(state.modelError)
            }
            if (state.serialNumberError != null) {
                serialNumber.error = getString(state.serialNumberError)
            }
        })
        viewModel.result.observe(viewLifecycleOwner, Observer {
            val result = it ?: return@Observer

            loading.visibility = View.GONE
            if (result.error != null) {
                showCreationError(result.error)
            } else if (result.success != null) {
                handleCreationSuccess(result.success)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun handleCreationSuccess(machine: Machine?) {
        val machine = machine!!

        val welcome = getString(R.string.welcome, machine.serialNumber)

        Toast.makeText(
            context,
            welcome,
            Toast.LENGTH_LONG
        ).show()

        findNavController().popBackStack()
    }

    private fun showCreationError(@StringRes errorString: Int) {
        Toast.makeText(context, errorString, Toast.LENGTH_SHORT).show()
    }
}