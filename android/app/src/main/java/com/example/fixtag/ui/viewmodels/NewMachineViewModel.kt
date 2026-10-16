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
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid


data class NewMachineFormState(
    val organizationError: Int? = null,
    val modelError: Int? = null,
    val serialNumberError: Int? = null,
    val isDataValid: Boolean = false
)

data class NewMachineFormResult(
    val success: Machine? = null,
    val error: Int? = null
)

class NewMachineViewModel(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
): InputBaseViewModel() {

    // one id per form, kept across rotation and process death
    private val machineId: String = savedStateHandle[MACHINE_ID_HANDLE]
        ?: Uuid.random().toString().also { savedStateHandle[MACHINE_ID_HANDLE] = it }

    private val _form = MutableLiveData<NewMachineFormState>()
    val form: LiveData<NewMachineFormState> = _form

    private val _result = MutableLiveData<NewMachineFormResult>()
    val result: LiveData<NewMachineFormResult> = _result

    private var _organizationId: StateFlow<String?> = savedStateHandle.getStateFlow(ORGANIZATION_ID_HANDLE, null)

    // the chosen client's name for the form, looked up from Room
    val organization: StateFlow<Organization?> =
        combine(_organizationId, repository.observeOrganizations()) { id, all -> all.find { it.id.toString() == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val _model: MutableLiveData<String?> = savedStateHandle.getLiveData(MACHINE_MODEL_HANDLE, null)
    val _serialNumber: MutableLiveData<String?> = savedStateHandle.getLiveData(
        MACHINE_SERIAL_NUMBER_HANDLE, null
    )

    // Submit function to
    fun submit() {
        // State.* comes from `InputBaseViewModel`
        if (state.value != State.Editing) return // ignores double taps
        state.value = State.Saving
        viewModelScope.launch {
            val machine = Machine.create(
                Uuid.parse(machineId),
                Uuid.parse(_organizationId.value!!),
                _serialNumber.value!!,
                _model.value!!
            )
            repository.createMachine(machine)
            state.value = State.Saved
        }
    }

    fun formDataChanged(organizationId: String, model: String, serialNumber: String) {
        var organizationIdError: Int? = null
        var modelError: Int? = null
        var serialNumberError: Int? = null

        if (organizationId.isEmpty()) {
            organizationIdError = R.string.empty_model_error
            return
        }
        savedStateHandle[ORGANIZATION_ID_HANDLE] = organizationId

        if (model.isEmpty()) {
            modelError = R.string.empty_model_error
            return
        }
        _model.value = model

        if (serialNumber.isEmpty()) {
            serialNumberError = R.string.empty_model_error
            return
        }
        _serialNumber.value = serialNumber

        if (organizationIdError != null || modelError != null || serialNumberError != null) {
            _form.value = NewMachineFormState(
                organizationIdError,
                modelError,
                serialNumberError
            )
            return
        }

        _form.value = NewMachineFormState(
            isDataValid = true
        )
    }
}