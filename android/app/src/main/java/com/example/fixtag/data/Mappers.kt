package com.example.fixtag.data

import com.example.fixtag.api.CreateIncidentRequestDto
import com.example.fixtag.api.UpdateIncidentRequestDto
import com.example.fixtag.api.model.AttachmentDto
import com.example.fixtag.api.model.IncidentDto
import com.example.fixtag.api.model.IncidentUpdateDto
import com.example.fixtag.api.model.MachineDto
import com.example.fixtag.data.model.Attachment
import com.example.fixtag.data.model.AttachmentKind
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.data.model.IncidentUpdate
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.SyncStatus
import kotlin.time.Instant
import kotlin.uuid.Uuid

fun MachineDto.toEntity(): Machine {
    return Machine(
        Uuid.parse(id),
        Uuid.parse(organizationId),
        serialNumber,
        model
    )
}

fun AttachmentDto.toEntity(
    incidentId: Uuid,
): Attachment {
    return Attachment(
        id = Uuid.parse(id),
        incidentId = incidentId,
        kind = AttachmentKind.fromCustomString(kind),
        contentType = contentType,
        sizeBytes = sizeBytes,
        path = "", // TODO ? need to fetch them
        syncStatus = SyncStatus.Synced,
        fetchStatus = SyncStatus.Pending
    )
}


fun IncidentUpdateDto.toEntity(incidentId: Uuid): IncidentUpdate {
    return IncidentUpdate(
        id = Uuid.parse(id),
        incidentId = incidentId,
        authorName = authorName,
        message = message,
        newStatus = newStatus,
        syncStatus = SyncStatus.Synced, // this comes from the API
    )
}

fun IncidentDto.toEntity(id: Uuid): Incident {
    return Incident(
        id = id,
        organizationId = Uuid.parse(organizationId),
        machineId = Uuid.parse(machineId),
        authorName = authorName,
        description = description,
        severity = IncidentSeverity.fromCustomString(severity),
        status = IncidentStatus.fromCustomString(status),
        latitude = latitude,
        longitude = longitude,
        accuracyM = accuracyM,
        reportedAt = Instant.parse(reportedAt),
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
        syncStatus = SyncStatus.Synced, // this comes from the API
    )
}

fun Incident.toRequest(): CreateIncidentRequestDto {
    return CreateIncidentRequestDto(
        machineId = machineId.toString(),
        description = description,
        severity = IncidentSeverity.toCustomString(severity),
        latitude = latitude,
        longitude = longitude,
        accuracyM = accuracyM,
        reportedAt = reportedAt.toEpochMilliseconds().toString()
    )
}

fun IncidentUpdate.toRequest(): UpdateIncidentRequestDto {
    return UpdateIncidentRequestDto(
        id = id.toString(),
        message = message,
        newStatus = newStatus
    )
}