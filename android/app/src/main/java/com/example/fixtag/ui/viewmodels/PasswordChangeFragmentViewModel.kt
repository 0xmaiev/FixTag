package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PasswordChangeFormState(
    val currentPasswordError: Int? = null,
    val newPasswordError: Int? = null,
) : FormState() {
    val isDataValid get() = currentPasswordError == null && newPasswordError == null
}

class PasswordChangeFragmentViewModel(private val repository: Repository): ViewModel()  {

    sealed interface State {
        data object Editing : State
        data object Saving : State
        data object Failed : State                    // no answer from the server: try again
        data class Changed(val user: User) : State    // signed in with the new password
    }

    private val _form = MutableStateFlow(PasswordChangeFormState())
    val form: StateFlow<PasswordChangeFormState> = _form.asStateFlow()

    private val _state = MutableStateFlow<State>(State.Editing)
    val state: StateFlow<State> = _state.asStateFlow()

    fun submit(currentPassword: String, newPassword: String) {
        _form.value = validate(currentPassword, newPassword)
        if (!_form.value.isDataValid || _state.value == State.Saving) return  // ignores double taps
        _state.value = State.Saving
        viewModelScope.launch {
            _state.value = when (val result = repository.changePassword(currentPassword, newPassword)) {
                is Repository.PasswordChange.Changed -> State.Changed(result.user)
                Repository.PasswordChange.WrongPassword -> {
                    _form.value = PasswordChangeFormState(currentPasswordError = R.string.current_password_wrong_error)
                    State.Editing
                }
                Repository.PasswordChange.Failed -> State.Failed
            }
        }
    }

    // the server's own rules, so the only 400 it can still answer is a wrong current password
    private fun validate(currentPassword: String, newPassword: String) = PasswordChangeFormState(
        currentPasswordError = if (currentPassword.isEmpty()) R.string.current_password_empty_error else null,
        newPasswordError = when {
            // the server counts characters, and an emoji is two Chars in Kotlin
            newPassword.codePointCount(0, newPassword.length) < MIN_PASSWORD_LENGTH ->
                R.string.new_password_too_short_error
            newPassword == currentPassword -> R.string.new_password_unchanged_error
            else -> null
        },
    )

    companion object {
        const val MIN_PASSWORD_LENGTH = 8  // the server's MIN_PASSWORD_LEN, also in new_password_too_short_error
    }
}