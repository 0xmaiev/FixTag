package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.User
import com.example.fixtag.ui.ORGANIZATION_ID_HANDLE
import com.example.fixtag.ui.ORGANIZATION_ID_KEY
import com.example.fixtag.ui.ORGANIZATION_NAME_HANDLE
import com.example.fixtag.ui.login.LoginFormState
import com.example.fixtag.ui.login.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class NewOrganizationFormState(
    val nameError: Int? = null,
    val isDataValid: Boolean = false
)

class NewOrganizationViewModel(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
): InputBaseViewModel() {
    // one id per form, kept across rotation and process death
    private val organizationId: String = savedStateHandle[ORGANIZATION_ID_HANDLE]
        ?: Uuid.random().toString().also { savedStateHandle[ORGANIZATION_ID_HANDLE] = it }

    private val _form = MutableStateFlow(NewOrganizationFormState())
    val form: StateFlow<NewOrganizationFormState> = _form.asStateFlow()

    fun submit(name: String) {
        // validate
        _form.value = validate(name)
        // early exit
        if (!_form.value.isDataValid || state.value != State.Editing) return
        state.value = State.Saving
        viewModelScope.launch {
            repository.createOrganization(Organization.create(Uuid.parse(organizationId), name.trim()))
            state.value = State.Saved
        }
    }

    // the server will trim and reject empty names, so "   " has to fail here too
    private fun validate(name: String) =
        if (name.isBlank()) NewOrganizationFormState(nameError = R.string.empty_name_error)
        else NewOrganizationFormState(isDataValid = true)

    fun formDataChanged(name: String) {
        _form.value = validate(name)
    }
}