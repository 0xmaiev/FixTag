package com.example.fixtag.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.showTemporaryPassword
import com.example.fixtag.ui.viewmodels.InputBaseViewModel
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
    private lateinit var nif: EditText
    private lateinit var managerName: EditText
    private lateinit var managerEmail: EditText
    // the state stays Saved, and each return to the screen collects it again
    private var passwordDialog: AlertDialog? = null

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
        nif = view.findViewById(R.id.nif)
        managerName = view.findViewById(R.id.managerName)
        managerEmail = view.findViewById(R.id.managerEmail)

        // register the client
        createOrganization.setOnClickListener { submit() }
        managerEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) submit()
            false
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // the button stays enabled: submit() checks again, and typing clears an error
                    viewModel.form.collect { form ->
                        name.error = form.nameError?.let { getString(it) }
                        nif.error = form.nifError?.let { getString(it) }
                        managerName.error = form.managerNameError?.let { getString(it) }
                        managerEmail.error = form.managerEmailError?.let { getString(it) }
                    }
                }
                launch {
                    viewModel.errors.collect { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        if (state == InputBaseViewModel.State.Saved) onSaved()
                    }
                }
            }
        }
    }

    private fun onSaved() {
        val registration = viewModel.registration
        val password = (registration as? Repository.Registration.Registered)?.temporaryPassword
        if (registration is Repository.Registration.Registered && password != null) {
            // the server shows the new gestor's password only now: the form stays until it's been read
            if (passwordDialog?.isShowing != true) {
                passwordDialog = showTemporaryPassword(registration.manager, password) {
                    findNavController().popBackStack()
                }
            }
            return
        }
        val message = when (registration) {
            is Repository.Registration.Linked -> getString(R.string.client_linked, registration.client.name)
            else -> getString(R.string.organization_saved)
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
    }

    private fun submit() = viewModel.submit(
        name.text.toString(),
        nif.text.toString(),
        managerName.text.toString(),
        managerEmail.text.toString(),
    )
}
