package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.Organization
import com.example.fixtag.ui.MACHINE_ID_HANDLE
import com.example.fixtag.ui.MACHINE_ID_KEY
import com.example.fixtag.ui.MACHINE_MODEL_HANDLE
import com.example.fixtag.ui.MACHINE_SERIAL_NUMBER_HANDLE
import com.example.fixtag.ui.ORGANIZATION_ID_HANDLE
import com.example.fixtag.ui.ORGANIZATION_ID_KEY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid


data class NewMachineFormState(
    val organizationError: Int? = null,
    val modelError: Int? = null,
    val serialNumberError: Int? = null
) {
    val isDataValid get() = organizationError == null && modelError == null && serialNumberError == null
}


class NewMachineViewModel(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
): InputBaseViewModel() {

    // one id per form, kept across rotation and process death
    private val machineId: String = savedStateHandle[MACHINE_ID_HANDLE]
        ?: Uuid.random().toString().also { savedStateHandle[MACHINE_ID_HANDLE] = it }

    private val _form = MutableStateFlow(NewMachineFormState())
    val form: StateFlow<NewMachineFormState> = _form.asStateFlow()

    // the dialog's pick is the one form value no view keeps for us
    private val organizationId: StateFlow<String?> = savedStateHandle.getStateFlow(ORGANIZATION_ID_HANDLE, null)

    // the chosen client's name for the form, looked up from Room
    val organization: StateFlow<Organization?> =
        combine(organizationId, repository.observeOrganizations()) { id, all -> all.find { it.id.toString() == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Submit function to
    fun submit(model: String, serialNumber: String) {
        val organizationId = organizationId.value
        _form.value = validate(organizationId, model, serialNumber)
        // early exit - State.* comes from `InputBaseViewModel`
        if (!_form.value.isDataValid || organizationId == null || state.value != State.Editing) return  // ignores double taps
        state.value = State.Saving
        viewModelScope.launch {
            repository.createMachine(Machine.create(
                Uuid.parse(machineId),
                Uuid.parse(organizationId),
                serialNumber.trim().uppercase(), // normalizing serial alphanumeric value
                model.trim()
            ))
            state.value = State.Saved
        }
    }

    // the server will trim and reject empty names, so "   " has to fail here too
    private fun validate(organizationId: String?, model: String, serialNumber: String): NewMachineFormState {
        var organizationError: Int? = null
        var modelError: Int? = null
        var serialNumberError: Int? = null

        if (organizationId == null) { organizationError = R.string.empty_organization_error }
        if (model.isBlank()) { modelError = R.string.empty_model_error }
        if (serialNumber.isBlank()) { serialNumberError = R.string.empty_serial_number_error }

        return NewMachineFormState(
            organizationError,
            modelError,
            serialNumberError
        )
    }

    fun selectOrganization(id: String) {
        savedStateHandle[ORGANIZATION_ID_HANDLE] = id
        _form.update { it.copy(organizationError = null) } // only clears the error this fixes
    }
}