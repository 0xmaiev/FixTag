package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.fixtag.ATTACHMENTS_DIR
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Attachment
import com.example.fixtag.data.model.AttachmentKind
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.SyncStatus
import com.example.fixtag.ui.AUDIOS_HANDLE
import com.example.fixtag.ui.GEO_ACCURACY_HANDLE
import com.example.fixtag.ui.GEO_LATITUDE_HANDLE
import com.example.fixtag.ui.GEO_LONGITUDE_HANDLE
import com.example.fixtag.ui.INCIDENT_ID_HANDLE
import com.example.fixtag.ui.MACHINE_ID_HANDLE
import com.example.fixtag.ui.PENDING_AUDIO_HANDLE
import com.example.fixtag.ui.PENDING_PHOTO_HANDLE
import com.example.fixtag.ui.PENDING_VIDEO_HANDLE
import com.example.fixtag.ui.PHOTOS_HANDLE
import com.example.fixtag.ui.VIDEOS_HANDLE
import com.example.fixtag.ui.adapters.AttachmentItem
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.uuid.Uuid


data class NewIncidentFormState(
    val machineError: Int? = null,
    val descriptionError: Int? = null
) {
    val isDataValid get() = machineError == null && descriptionError == null
}

class NewIncidentViewModel(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
) : InputBaseViewModel() {

    // one id per form, kept across rotation and process death
    private val incidentId: String = savedStateHandle[INCIDENT_ID_HANDLE]
        ?: Uuid.random().toString().also { savedStateHandle[INCIDENT_ID_HANDLE] = it }

    private val _form = MutableStateFlow(NewIncidentFormState())
    val form: StateFlow<NewIncidentFormState> = _form.asStateFlow()

    // machines to pass to the selection dialog and for the flow below
    val machines: Flow<List<Machine>> = repository.observeMachines()
    // the dialog's pick is the one form value no view keeps for us so we have to store it here
    private val machineId: StateFlow<String?> = savedStateHandle.getStateFlow(MACHINE_ID_HANDLE, null)
    // the chosen client's name for the form, looked up from Room
    val machine: StateFlow<Machine?> =
        combine(machineId, machines) { id, all -> all.find { it.id.toString() == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val attachments: StateFlow<List<AttachmentItem>> =
        combine(AttachmentKind.entries.map { kind ->
            files(kind).map { paths -> paths.map { AttachmentItem(File(it).nameWithoutExtension, kind, it) } }
        }) { perKind -> perKind.flatMap { it } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeAttachment(item: AttachmentItem) {
        val path = item.path ?: return
        savedStateHandle[listKey(item.kind)] = ArrayList(files(item.kind).value - path)
        File(path).delete()
    }

    val latitude: StateFlow<Double?> = savedStateHandle.getStateFlow(GEO_LATITUDE_HANDLE, null)
    val longitude: StateFlow<Double?> = savedStateHandle.getStateFlow(GEO_LONGITUDE_HANDLE, null)
    val accuracyM: StateFlow<Float?> = savedStateHandle.getStateFlow(GEO_ACCURACY_HANDLE, null)

    val audios: StateFlow<List<String>> = savedStateHandle.getStateFlow(
        listKey(AttachmentKind.Audio), emptyList())

    private fun listKey(kind: AttachmentKind) = "files_${kind.name}"
    private fun pendingKey(kind: AttachmentKind) = "pending_${kind.name}"

    private fun validate(machine: Machine?, description: String): NewIncidentFormState {
        var machineError: Int? = null
        var descriptionError: Int? = null

        if (machine == null) { machineError = R.string.empty_machine_error }
        if (description.isBlank() && audios.value.isEmpty()) {
            descriptionError = R.string.empty_description_error
        }

        return NewIncidentFormState(
            machineError,
            descriptionError
        )
    }

    fun submit(description: String, severityChips: ChipGroup) {
        val machine = machine.value
        val user = repository.user
        _form.value = validate(machine, description)
        val severity = when(severityChips.checkedChipId) {
            R.id.severityLowChip -> IncidentSeverity.Low
            R.id.severityMediumChip -> IncidentSeverity.Medium
            R.id.severityCriticalChip -> IncidentSeverity.Critical
            else -> return // shouldn't ever happen
        }
        // early exit - State.* comes from `InputBaseViewModel`
        if (!_form.value.isDataValid || machine == null ||
            user == null || state.value != State.Editing) return  // ignores double taps
        state.value = State.Saving
        viewModelScope.launch {
            val id = Uuid.parse(incidentId)  // you were passing machine.id here
            val incident = Incident.create(id, machine.organizationId, machine.id, user.name, description,
                severity, latitude.value, longitude.value, accuracyM.value)
            val attachments = AttachmentKind.entries.flatMap { kind ->
                files(kind).value.map { path ->
                    val file = File(path)
                    Attachment.create(
                        id = Uuid.parse(file.nameWithoutExtension),
                        incidentId = id,
                        kind = kind,
                        contentType = kind.contentType,  // the server picks the kind from this; "media/" gets a 415
                        sizeBytes = file.length().toInt(),
                        path = path,
                    ).apply { fetchStatus = SyncStatus.Synced }  // already on this device, nothing to download
                }
            }
            repository.createIncident(incident, attachments)
            state.value = State.Saved
        }
    }

    fun files(kind: AttachmentKind): StateFlow<List<String>> =
        savedStateHandle.getStateFlow(listKey(kind), emptyList())

    // the camera app can get our process killed, so remember the file before launching it
    fun newAttachmentFile(kind: AttachmentKind, filesDir: File): File {
        val dir = File(filesDir, ATTACHMENTS_DIR).apply { mkdirs() }
        return File(dir, "${Uuid.random()}.${kind.extension}")  // the file name is the attachment id
            .also { savedStateHandle[pendingKey(kind)] = it.absolutePath }
    }

    fun onCaptured(kind: AttachmentKind, saved: Boolean) {
        val path = savedStateHandle.remove<String>(pendingKey(kind)) ?: return
        if (saved && File(path).length() > 0) {
            savedStateHandle[listKey(kind)] = ArrayList(files(kind).value + path)
        }
        else File(path).delete()
    }

    fun setGeolocationInfo(latitude: Double?, longitude: Double?, accuracy: Float?) {
        savedStateHandle[GEO_LATITUDE_HANDLE] = latitude
        savedStateHandle[GEO_LONGITUDE_HANDLE] = longitude
        savedStateHandle[GEO_ACCURACY_HANDLE] = accuracy
    }

    fun selectMachine(id: String) {
        savedStateHandle[MACHINE_ID_HANDLE] = id
        _form.update { it.copy(machineError = null) } // only clears the error this fixes
    }
}