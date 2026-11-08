package com.example.fixtag.ui.fragments

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fixtag.AppViewModelFactory
import com.example.fixtag.FixTag
import com.example.fixtag.R
import com.example.fixtag.ui.login.LoginViewModel
import com.example.fixtag.ui.login.LoginViewModelFactory
import com.example.fixtag.ui.viewmodels.PasswordChangeFragmentViewModel
import com.example.fixtag.ui.viewmodels.PasswordChangeFragmentViewModel.State
import kotlinx.coroutines.launch

// shown after /login when the account is still on its temporary password
class PasswordChangeFragment: Fragment(R.layout.fragment_password_change) {

    private val viewModel: PasswordChangeFragmentViewModel by viewModels {
        AppViewModelFactory((requireContext().applicationContext as FixTag).repository)
    }

    // the activity's, which opens the app once the password has changed
    private val loginViewModel: LoginViewModel by activityViewModels {
        LoginViewModelFactory((requireContext().applicationContext as FixTag).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentPassword = view.findViewById<EditText>(R.id.currentPassword)
        val newPassword = view.findViewById<EditText>(R.id.newPassword)
        val confirm = view.findViewById<Button>(R.id.confirm)
        val loading = view.findViewById<ProgressBar>(R.id.loading)
        val failed = view.findViewById<TextView>(R.id.failed)

        fun submit() = viewModel.submit(currentPassword.text.toString(), newPassword.text.toString())
        confirm.setOnClickListener { submit() }
        newPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) submit()
            false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // the button stays enabled: submit() checks again, and typing clears an error
                    viewModel.form.collect { form ->
                        currentPassword.error = form.currentPasswordError?.let { getString(it) }
                        newPassword.error = form.newPasswordError?.let { getString(it) }
                    }
                }
                launch {
                    viewModel.state.collect { state ->
                        confirm.isEnabled = state != State.Saving
                        loading.isVisible = state == State.Saving
                        // on the screen rather than a toast, so it's still there after a rotation
                        failed.isVisible = state == State.Failed
                        if (state is State.Changed) {
                            Toast.makeText(requireContext(), R.string.password_changed, Toast.LENGTH_SHORT).show()
                            loginViewModel.signedIn(state.user)
                        }
                    }
                }
            }
        }
    }
}