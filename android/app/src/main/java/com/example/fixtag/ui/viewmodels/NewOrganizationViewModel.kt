package com.example.fixtag.ui.viewmodels

import androidx.datastore.core.wrapExceptionIfDueToDirectBoot
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
    val nifError: Int? = null,
    val managerEmailError: Int? = null
) {
    val isDataValid get() = nameError == null && nifError == null && managerEmailError == null
}

class NewOrganizationViewModel(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
): InputBaseViewModel() {
    // one id per form, kept across rotation and process death
    private val organizationId: String = savedStateHandle[ORGANIZATION_ID_HANDLE]
        ?: Uuid.random().toString().also { savedStateHandle[ORGANIZATION_ID_HANDLE] = it }

    private val _form = MutableStateFlow(NewOrganizationFormState())
    val form: StateFlow<NewOrganizationFormState> = _form.asStateFlow()

    fun submit(name: String, nif: String, managerEmail: String) {
        // validate
        _form.value = validate(name, nif, managerEmail)
        // early exit
        if (!_form.value.isDataValid || state.value != State.Editing) return
        state.value = State.Saving
        viewModelScope.launch {
            repository.createOrganization(Organization.create(
                Uuid.parse(organizationId),
                name.trim(),
                nif
            ))
            state.value = State.Saved
        }
    }

    // the server will trim and reject empty names, so "   " has to fail here too
    private fun validate(name: String, nif: String, managerEmail: String): NewOrganizationFormState {
        var nameError: Int? = null
        var nifError: Int? = null
        var managerEmailError: Int? = null

        if (name.isBlank()) { nameError = R.string.empty_name_error }
        if (managerEmail.isBlank()) { managerEmailError = R.string.invalid_email }
        if (nif.isBlank()) { nifError = R.string.empty_nif_error }
        if (!isValidNif(nif)) { nifError = R.string.invalid_nif_error }

        return NewOrganizationFormState(
            nameError,
            nifError,
            managerEmailError
        )
    }

    fun isValidNif(nif: String): Boolean {
        if (nif.length != 9 || !nif.all { it in '0'..'9' }) return false
        val sum = (0 until 8).sumOf { (nif[it] - '0') * (9 - it) }
        val check = (11 - sum % 11).let { if (it >= 10) 0 else it }
        return nif[8] - '0' == check
    }

    fun formDataChanged(name: String, nif: String, managerEmail: String) {
        _form.value = validate(name, nif, managerEmail)
    }
}