package com.example.fixtag.ui.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
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
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.User
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.afterTextChanged
import com.example.fixtag.ui.main.MainActivity
import com.example.fixtag.ui.viewmodels.NewIncidentViewModel
import com.example.fixtag.ui.viewmodels.NewOrganizationViewModel

class NewOrganizationFragment: Fragment() {

    companion object {
        fun newInstance() = NewOrganizationFragment()
    }

    private val viewModel: NewOrganizationViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        ItemViewModelFactory(repository)
    }

    private lateinit var back: Button
    private lateinit var createOrganization: Button
    private lateinit var name: EditText
    private lateinit var loading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_organization, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back = view.findViewById(R.id.backButton)
        createOrganization = view.findViewById(R.id.createOrganizationButton)
        name = view.findViewById(R.id.name)
        loading = view.findViewById(R.id.loading)

        // react to ui interactions

        // navigate back
        back.setOnClickListener { findNavController().navigateUp() }
        // create the machine
        createOrganization.setOnClickListener { viewModel.submit() }
        name.apply {
            // set afterTextChanged on the text edit field
            afterTextChanged {
                viewModel.formDataChanged(name.text.toString())
            }
            // react to enter key being pressed on the text input
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> viewModel.submit()
                }
                false
            }
        }

        // react to form validation
        viewModel.form.observe(viewLifecycleOwner, Observer {
            val state = it ?: return@Observer

            // disable login button unless both username / password is valid
            createOrganization.isEnabled = state.isDataValid

            if (state.nameError != null) {
                name.error = getString(state.nameError)
            }
        })
        // react to result
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

    private fun handleCreationSuccess(organization: Organization?) {
        val organization = organization!!

        val welcome = getString(R.string.welcome, organization.name)

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