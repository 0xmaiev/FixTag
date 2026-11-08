package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Attachment
import com.example.fixtag.data.model.IncidentAndMachine
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.data.model.IncidentUpdate
import com.example.fixtag.data.model.TechnicianWithLoad
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.isSupport
import com.example.fixtag.ui.INCIDENT_ID_KEY
import com.example.fixtag.ui.adapters.AttachmentItem
import com.example.fixtag.ui.adapters.toItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

// what the signed-in user may do with this incident, the same checks the server makes
data class IncidentPermissions(
    val edit: Boolean = false,          // PUT /incidents/{id}: only the author
    val changeStatus: Boolean = false,  // support staff, or the technician it's assigned to
    val assign: Boolean = false,        // support staff only
)

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

    // follows the signed-in user too, which can arrive after the incident when Android restores the app
    val permissions: StateFlow<IncidentPermissions> =
        combine(incident, repository.signedIn) { data, user ->
            val incident = data?.incident
            if (incident == null || user == null) IncidentPermissions()
            else IncidentPermissions(
                edit = incident.authorId == user.id.toString(),
                changeStatus = user.isSupport || incident.assignedTo == user.id,
                assign = user.isSupport,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IncidentPermissions())

    // the technicians the signed-in support staff can pick, with how many open incidents each has
    suspend fun technicians(): List<TechnicianWithLoad> = repository.observeTechnicians().first()

    // false when there's nothing to send: the server wants a message, a new status or a technician
    fun postUpdate(message: String, status: IncidentStatus?): Boolean {
        val text = message.trim().ifEmpty { null }
        if (text == null && status == null) return false
        save { author -> IncidentUpdate.create(Uuid.random(), id, author.name, text, status, null, null) }
        return true
    }

    fun assign(technician: User) = save { author ->
        IncidentUpdate.create(Uuid.random(), id, author.name, null, null, technician.id, technician.name)
    }

    private fun save(update: (author: User) -> IncidentUpdate) {
        val author = repository.user ?: return
        viewModelScope.launch { repository.createIncidentUpdate(update(author)) }
    }
}
