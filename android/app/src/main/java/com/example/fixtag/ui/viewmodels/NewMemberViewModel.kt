package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserRole
import com.example.fixtag.ui.ORGANIZATION_ID_HANDLE
import com.example.fixtag.ui.USER_ID_HANDLE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid


data class NewMemberFormState(
    val organizationError: Int? = null,
    val nameError: Int? = null,
    val emailError: Int? = null,
) {
    val isDataValid get() = organizationError == null && nameError == null && emailError == null
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

    // the dialog's pick is the one form value no view keeps for us
    private val organizationId: StateFlow<String?> = savedStateHandle.getStateFlow(ORGANIZATION_ID_HANDLE, null)

    // the chosen client's name for the form, looked up from Room
    val organization: StateFlow<Organization?> =
        combine(organizationId, repository.observeOrganizations()) { id, all -> all.find { it.id.toString() == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Submit function to
    fun submit(name: String, email: String, role: UserRole,) {
        val organization = organization.value
        _form.value = validate(organization, name, email)
        // early exit - State.* comes from `InputBaseViewModel`
        if (!_form.value.isDataValid || organization == null || state.value != State.Editing) return  // ignores double taps
        state.value = State.Saving
        viewModelScope.launch {
            repository.createUser(
                User.create(
                Uuid.parse(userId),
                organization.id,
                    organization.kind,
                    name.trim(),
                    email.trim(),
                    role,
            ))
            state.value = State.Saved
        }
    }

    // the server will trim and reject empty names, so "   " has to fail here too
    private fun validate(organization: Organization?, name: String, email: String): NewMemberFormState {
        var organizationError: Int? = null
        var nameError: Int? = null
        var emailError: Int? = null

        if (organization == null) { organizationError = R.string.empty_organization_error }
        if (name.isBlank()) { nameError = R.string.empty_name_error }
        if (email.isBlank()) { emailError = R.string.empty_name_error } // TODO: Change

        return NewMemberFormState(
            organizationError,
            nameError,
            emailError,
        )
    }

    fun selectOrganization(id: String) {
        savedStateHandle[ORGANIZATION_ID_HANDLE] = id
        _form.update { it.copy(organizationError = null) } // only clears the error this fixes
    }
}