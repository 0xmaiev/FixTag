package com.example.fixtag.ui.viewmodels

import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserRole
import com.example.fixtag.data.model.creatableRoles
import com.example.fixtag.ui.ROLE_HANDLE
import com.example.fixtag.ui.USER_ID_HANDLE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid


data class NewMemberFormState(
    val nameError: Int? = null,
    val emailError: Int? = null,
) : FormState() {
    val isDataValid get() = nameError == null && emailError == null
}


class NewMemberViewModel(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
): InputBaseViewModel() {

    // one id per form, kept across rotation and process death
    private val userId: String = savedStateHandle[USER_ID_HANDLE]
        ?: Uuid.random().toString().also { savedStateHandle[USER_ID_HANDLE] = it }

    private val _form = MutableStateFlow(NewMemberFormState())
    val form: StateFlow<NewMemberFormState> = _form.asStateFlow()

    // the roles the signed-in user may create (User.canCreate). The account always goes in their
    // own organization, as POST /users does, so there's no organization to pick
    val roles: List<UserRole> = repository.user?.creatableRoles.orEmpty()

    // the first role until the user picks another, kept across rotation
    val role: StateFlow<UserRole?> = savedStateHandle.getStateFlow(ROLE_HANDLE, roles.firstOrNull())

    fun selectRole(role: UserRole) {
        savedStateHandle[ROLE_HANDLE] = role
    }

    fun submit(name: String, email: String) {
        val creator = repository.user
        val role = role.value
        _form.value = validate(name, email)
        // early exit - State.* comes from `InputBaseViewModel`
        if (!_form.value.isDataValid || creator == null || role == null || state.value != State.Editing) return  // ignores double taps
        state.value = State.Saving
        viewModelScope.launch {
            // unique on the server too, which would answer 409
            if (repository.isEmailTaken(email.trim())) {
                _form.value = NewMemberFormState(emailError = R.string.email_taken_error)
                state.value = State.Editing
                return@launch
            }
            repository.createUser(User.create(
                Uuid.parse(userId),
                creator.organizationId,
                creator.organizationKind,
                name.trim(),
                email.trim(),
                role,
            ))
            state.value = State.Saved
        }
    }

    // the server will trim and reject empty names, so "   " has to fail here too
    private fun validate(name: String, email: String) = NewMemberFormState(
        nameError = if (name.isBlank()) R.string.empty_name_error else null,
        emailError = if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) R.string.invalid_email else null,
    )
}
