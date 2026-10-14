package com.example.fixtag.ui.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.Machine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

class MachinesViewModel(private val repository: Repository) : ViewModel() {

    val machines: StateFlow<List<Machine>> = repository.observeMachines()
        .stateIn(
            scope = viewModelScope,
            // Keeps the flow active for 5 seconds after UI rotates/disconnects
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // The initial state before Room emits data
        )

    init { }
}