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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.User
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.afterTextChanged
import com.example.fixtag.ui.main.MainActivity
import com.example.fixtag.ui.viewmodels.InputBaseViewModel
import com.example.fixtag.ui.viewmodels.NewIncidentViewModel
import com.example.fixtag.ui.viewmodels.NewOrganizationViewModel
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class NewOrganizationFragment: Fragment() {

    companion object {
        fun newInstance() = NewOrganizationFragment()
    }

    private val viewModel: NewOrganizationViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        ItemViewModelFactory(repository)
    }

    private lateinit var createOrganization: Button
    private lateinit var name: EditText

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

        view.findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { findNavController().navigateUp() }
        createOrganization = view.findViewById(R.id.createOrganizationButton)
        name = view.findViewById(R.id.name)

        // react to ui interactions

        // create the machine
        createOrganization.setOnClickListener { viewModel.submit(name.text.toString()) }
        name.apply {
            // set afterTextChanged on the text edit field
            afterTextChanged {
                viewModel.formDataChanged(name.text.toString())
            }
            // react to enter key being pressed on the text input
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> viewModel.submit(name.text.toString())
                }
                false
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.form.collect { form ->
                        createOrganization.isEnabled = form.isDataValid
                        name.error = form.nameError?.let { getString(it) }
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        if (state == InputBaseViewModel.State.Saved) {
                            Toast.makeText(requireContext(), R.string.organization_saved, Toast.LENGTH_SHORT).show()
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