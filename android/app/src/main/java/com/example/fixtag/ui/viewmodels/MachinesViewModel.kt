package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.MachineWithStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.collections.emptyList

class MachinesViewModel(private val repository: Repository) : ViewModel() {

    val uiState: StateFlow<List<MachineWithStats>> = repository.observeMachinesWithStats()
        .stateIn(
            scope = viewModelScope,
            // Keeps the flow active for 5 seconds after UI rotates/disconnects
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // The initial state before Room emits data
        )

    init { }

    suspend fun delete(machine: Machine) = repository.deletePendingMachine(machine.id)
}