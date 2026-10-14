package com.example.fixtag.ui.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Incident
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IncidentsViewModel(private val repository: Repository) : ViewModel() {
    val uiState: StateFlow<List<Incident>> = repository.observeIncidents()
        .stateIn(
            scope = viewModelScope,
            // Keeps the flow active for 5 seconds after UI rotates/disconnects
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // The initial state before Room emits data
        )

    init {
        // Trigger a background network sync immediately when screen loads
        viewModelScope.launch {
            repository.syncIncidents()
        }
    }
}