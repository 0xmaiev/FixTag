package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Attachment
import com.example.fixtag.data.model.IncidentAndMachine
import com.example.fixtag.data.model.IncidentUpdate
import com.example.fixtag.ui.INCIDENT_ID_KEY
import com.example.fixtag.ui.adapters.AttachmentItem
import com.example.fixtag.ui.adapters.toItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.uuid.Uuid


class IncidentDetailViewModel(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val id = Uuid.parse(checkNotNull(savedStateHandle[INCIDENT_ID_KEY]))

    val incident: StateFlow<IncidentAndMachine?> = repository.observeIncidentAndMachine(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),null)

    val updates: StateFlow<List<IncidentUpdate>> =
        repository.observeIncidentUpdates(id)
            .stateIn(
                scope = viewModelScope,
                // Keeps the flow active for 5 seconds after UI rotates/disconnects
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList() // The initial state before Room emits data
            )

    val attachments: StateFlow<List<AttachmentItem>> =
        repository.observeAttachments(id)
            .map { list -> list.map { it.toItem() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {

    }
}