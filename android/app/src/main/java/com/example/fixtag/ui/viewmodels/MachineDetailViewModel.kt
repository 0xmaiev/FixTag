package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.Machine
import com.example.fixtag.ui.MACHINE_ID_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.uuid.Uuid

class MachineDetailViewModel(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val id = Uuid.parse(checkNotNull(savedStateHandle[MACHINE_ID_KEY]))

    val machine: Flow<Machine?> =
        repository.observeMachine(id)
            .stateIn(
                scope = viewModelScope,
                // Keeps the flow active for 5 seconds after UI rotates/disconnects
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null // The initial state before Room emits data
            )

    val incidents: Flow<List<Incident>> =
        repository.observeIncidentsByMachine(id)
            .stateIn(
                scope = viewModelScope,
                // Keeps the flow active for 5 seconds after UI rotates/disconnects
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList() // The initial state before Room emits data
            )

    init { }
}