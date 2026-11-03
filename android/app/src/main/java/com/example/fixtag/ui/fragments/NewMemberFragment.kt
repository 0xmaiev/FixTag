package com.example.fixtag.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.data.model.UserRole
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.main.MainViewModel
import com.example.fixtag.ui.main.MainViewModelFactory
import com.example.fixtag.ui.viewmodels.InputBaseViewModel
import com.example.fixtag.ui.viewmodels.NewMemberViewModel
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class NewMemberFragment: Fragment() {

    companion object {
        fun newInstance() = NewMemberFragment()
    }

    private val mainViewModel: MainViewModel by activityViewModels {
        val app = requireContext().applicationContext as FixTag
        MainViewModelFactory(app.repository, app.syncScheduler)
    }

    private val viewModel: NewMemberViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        ItemViewModelFactory(repository)
    }

    private lateinit var createMember: Button
    private lateinit var name: TextView
    private lateinit var email: TextView
    private lateinit var organization: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_member, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { findNavController().navigateUp() }
        createMember = view.findViewById(R.id.createMemberButton)
        name = view.findViewById(R.id.name)
        email = view.findViewById(R.id.email)
        organization = view.findViewById(R.id.organization)

        // create the machine
        createMember.setOnClickListener {
            viewModel.submit(name.text.toString(), email.text.toString(), UserRole.Operador) // TODO:
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.form.collect { form ->
                        createMember.isEnabled = form.isDataValid
                        organization.error = form.organizationError?.let { getString(it) }
                        name.error = form.nameError?.let { getString(it) }
                        email.error = form.emailError?.let { getString(it) }
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
                                R.string.member_saved,
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().popBackStack()
                        }
                    }
                }
                launch {
                    mainViewModel.organizationId.collect {
                        value -> viewModel.selectOrganization(value.toString())
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}