package com.example.fixtag.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.ui.ItemViewModelFactory
import com.example.fixtag.ui.label
import com.example.fixtag.ui.viewmodels.InputBaseViewModel
import com.example.fixtag.ui.viewmodels.NewMemberViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class NewMemberFragment: Fragment() {

    companion object {
        fun newInstance() = NewMemberFragment()
    }

    private val viewModel: NewMemberViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        ItemViewModelFactory(repository)
    }

    private lateinit var createMember: Button
    private lateinit var name: TextView
    private lateinit var email: TextView
    private lateinit var roles: ChipGroup

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
        roles = view.findViewById(R.id.roles)

        // one chip per role this user may create; which one is checked lives in the view model
        for (role in viewModel.roles) {
            val chip = layoutInflater.inflate(R.layout.chip_choice, roles, false) as Chip
            chip.id = View.generateViewId()
            chip.setText(role.label())
            chip.isChecked = role == viewModel.role.value
            chip.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.selectRole(role) }
            roles.addView(chip)
        }

        // create the member
        createMember.setOnClickListener {
            viewModel.submit(name.text.toString(), email.text.toString())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // the button stays enabled: submit() checks again, and typing clears an error
                    viewModel.form.collect { form ->
                        name.error = form.nameError?.let { getString(it) }
                        email.error = form.emailError?.let { getString(it) }
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
            }
        }
    }
}
