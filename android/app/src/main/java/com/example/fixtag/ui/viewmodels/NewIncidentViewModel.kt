package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.Organization
import com.example.fixtag.ui.INCIDENT_ID_KEY
import com.example.fixtag.ui.MACHINE_ID_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid


data class FormState(
    val nameError: Int? = null,
    val isDataValid: Boolean = false
)

data class FormResult(
    val success: Organization? = null,
    val error: Int? = null
)

class NewIncidentViewModel(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
) : InputBaseViewModel() {
    // machines to pass to the selection dialog
    val machines: Flow<List<Machine>> = repository.observeMachines()

    // one id per form, kept across rotation and process death
    private val incidentId: String = savedStateHandle[INCIDENT_ID_KEY]
        ?: Uuid.random().toString().also { savedStateHandle[INCIDENT_ID_KEY] = it }

    // machine id is selectable by user so we're going to use mutable / live data
    private var _machineId: MutableLiveData<String?> = savedStateHandle.getLiveData(MACHINE_ID_KEY, null)
    val machineId: LiveData<String?> = _machineId

    fun submit() {
        if (machineId.value == null) return // can't create incident without machine id
        // State.* comes from `InputBaseViewModel`
        if (state.value != State.Editing) return // ignores double taps
        state.value = State.Saving
        viewModelScope.launch {
            // TODO:
            state.value = State.Saved
        }
    }
}