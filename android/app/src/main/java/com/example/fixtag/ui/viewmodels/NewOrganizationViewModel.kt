package com.example.fixtag.ui.viewmodels

import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Organization
import com.example.fixtag.ui.ORGANIZATION_ID_HANDLE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class NewOrganizationFormState(
    val nameError: Int? = null,
    val nifError: Int? = null,
    val managerNameError: Int? = null,
    val managerEmailError: Int? = null,
) : FormState() {
    val isDataValid get() =
        nameError == null && nifError == null && managerNameError == null && managerEmailError == null
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

    // set once saved: a new client, or one whose NIF was already registered and only got linked
    var registration: Repository.Registration? = null
        private set

    fun submit(name: String, nif: String, managerName: String, managerEmail: String) {
        // validate
        _form.value = validate(name, nif.trim(), managerName, managerEmail)
        // early exit
        if (!_form.value.isDataValid || state.value != State.Editing) return
        state.value = State.Saving
        viewModelScope.launch {
            val client = Organization.create(Uuid.parse(organizationId), name.trim(), nif.trim())
            val result = repository.registerClient(client, managerName.trim(), managerEmail.trim())
            when (result) {
                Repository.Registration.NifIsAManufacturer ->
                    _form.value = NewOrganizationFormState(nifError = R.string.nif_manufacturer_error)
                Repository.Registration.EmailTaken ->
                    _form.value = NewOrganizationFormState(managerEmailError = R.string.email_taken_error)
                else -> registration = result
            }
            state.value = if (registration == null) State.Editing else State.Saved
        }
    }

    // the server will trim and reject empty names, so "   " has to fail here too
    private fun validate(name: String, nif: String, managerName: String, managerEmail: String) =
        NewOrganizationFormState(
            nameError = if (name.isBlank()) R.string.empty_name_error else null,
            nifError = when {
                nif.isEmpty() -> R.string.empty_nif_error
                !isValidNif(nif) -> R.string.invalid_nif_error
                else -> null
            },
            managerNameError = if (managerName.isBlank()) R.string.empty_name_error else null,
            managerEmailError =
                if (!Patterns.EMAIL_ADDRESS.matcher(managerEmail.trim()).matches()) R.string.invalid_email else null,
        )

    fun isValidNif(nif: String): Boolean {
        if (nif.length != 9 || !nif.all { it in '0'..'9' }) return false
        val sum = (0 until 8).sumOf { (nif[it] - '0') * (9 - it) }
        val check = (11 - sum % 11).let { if (it >= 10) 0 else it }
        return nif[8] - '0' == check
    }
}
