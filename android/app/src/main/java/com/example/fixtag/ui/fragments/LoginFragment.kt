package com.example.fixtag.ui.fragments

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.BuildConfig
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.data.DemoSeed
import com.example.fixtag.data.model.OrganizationKind
import com.example.fixtag.data.model.User
import com.example.fixtag.ui.afterTextChanged
import com.example.fixtag.ui.label
import com.example.fixtag.ui.login.LoginViewModel
import com.example.fixtag.ui.login.LoginViewModelFactory
import com.example.fixtag.ui.viewmodels.LoginFragmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.time.Clock

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginFragmentViewModel by viewModels {
        val context = requireContext().applicationContext
        val repository = (context as FixTag).repository

        AppViewModelFactory(repository)
    }

    // the activity's, which decides where a signed-in user goes
    private val loginViewModel: LoginViewModel by activityViewModels {
        LoginViewModelFactory((requireContext().applicationContext as FixTag).repository)
    }

    private lateinit var demoClient: Button
    private lateinit var demoManufacturer: Button
    private lateinit var login: Button
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var loading: ProgressBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (BuildConfig.OFFLINE_DEMO) {
            view.findViewById<View>(R.id.LoginFormLayout).isVisible = false
            view.findViewById<LinearLayout>(R.id.demoProfiles).isVisible = true
            demoClient = view.findViewById(R.id.demoClient)
            demoManufacturer = view.findViewById(R.id.demoManufacturer)
            demoClient.setOnClickListener { pickDemoProfile(OrganizationKind.Cliente) }
            demoManufacturer.setOnClickListener { pickDemoProfile(OrganizationKind.Fabricante) }
        }

        username = view.findViewById(R.id.etEmail)
        password = view.findViewById(R.id.etPassword)
        login = view.findViewById(R.id.login)
        loading = view.findViewById(R.id.loading)

        viewModel.loginFormState.observe(viewLifecycleOwner, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.emailError != null) {
                username.error = getString(loginState.emailError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
        })

        viewModel.loginResult.observe(viewLifecycleOwner, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            } else if (loginResult.success != null) {
                handleLoginSuccess(loginResult.success)
            }
        })

        username.afterTextChanged {
            viewModel.loginDataChanged(
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                viewModel.loginDataChanged(
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        viewModel.login(
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }
        }

        login.setOnClickListener {
            loading.visibility = View.VISIBLE
            viewModel.login(username.text.toString(), password.text.toString())
        }
    }

    // every person in the demo seed on one side: client staff, or manufacturer staff
    private fun pickDemoProfile(kind: OrganizationKind) {
        val organizations = DemoSeed.organizations(Clock.System.now()).associate { it.id to it.name }
        val profiles = DemoSeed.users.filter { it.organizationKind == kind }
        val labels = profiles.map {
            "${it.name} · ${getString(it.role.label())}\n${organizations[it.organizationId]}"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.demo_pick_profile)
            .setItems(labels.toTypedArray()) { _, which ->
                viewModel.loginAsDemoProfile(profiles[which].id)
            }
            .show()
    }

    private fun handleLoginSuccess(user: User) {
        val welcome = getString(R.string.welcome, user.name)

        Toast.makeText(
            requireContext(),
            welcome,
            Toast.LENGTH_LONG
        ).show()

        // the main screen, or first the password change; the activity navigates either way
        loginViewModel.signedIn(user)
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(requireContext(), errorString, Toast.LENGTH_SHORT).show()
    }
}