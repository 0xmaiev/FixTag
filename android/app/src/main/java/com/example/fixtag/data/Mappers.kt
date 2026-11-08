package com.example.fixtag.data

import com.example.fixtag.api.CreateIncidentRequestDto
import com.example.fixtag.api.CreateMachineRequestDto
import com.example.fixtag.api.RenameOrganizationRequestDto
import com.example.fixtag.api.UpdateIncidentRequestDto
import com.example.fixtag.api.model.AttachmentDto
import com.example.fixtag.api.model.IncidentDto
import com.example.fixtag.api.model.IncidentUpdateDto
import com.example.fixtag.api.model.MachineDto
import com.example.fixtag.api.model.OrganizationDto
import com.example.fixtag.api.model.UserDto
import com.example.fixtag.data.model.Attachment
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentUpdate
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.SyncStatus
import com.example.fixtag.data.model.User
import kotlin.time.Instant
import kotlin.uuid.Uuid

fun OrganizationDto.toEntity(): Organization {
    return Organization(
        id = Uuid.parse(id),
        name = name,
        kind = kind,
        createdAt = Instant.parse(createdAt),
        syncStatus = SyncStatus.Synced,
        nif = nif
    )
}

fun UserDto.toEntity(): User {
    return User(
        id = Uuid.parse(id),
        organizationId = Uuid.parse(organizationId),
        organizationKind = organizationKind,
        name = name,
        email = email,
        role = role,
        mustChangePassword = mustChangePassword,
        syncStatus = SyncStatus.Synced,  // it came from the server
    )
}

fun MachineDto.toEntity(): Machine {
    return Machine(
        id = Uuid.parse(id),
        organizationId = Uuid.parse(organizationId),
        manufacturerId = Uuid.parse(manufacturerId),
        serialNumber = serialNumber,
        model = model,
        createdAt = Instant.parse(createdAt),
        syncStatus = SyncStatus.Synced,
    )
}

fun AttachmentDto.toEntity(
    incidentId: Uuid,
): Attachment {
    return Attachment(
        id = Uuid.parse(id),
        incidentId = incidentId,
        kind = kind,
        contentType = contentType,
        sizeBytes = sizeBytes,
        path = "",
        createdAt = Instant.parse(createdAt),
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
        assignedTo = assignedTo,
        assignedToName = assignedToName,
        createdAt = Instant.parse(createdAt),
        syncStatus = SyncStatus.Synced, // this comes from the API
    )
}

fun IncidentDto.toEntity(id: Uuid): Incident {
    return Incident(
        id = id,
        organizationId = Uuid.parse(organizationId),
        machineId = Uuid.parse(machineId),
        handledBy = Uuid.parse(handledBy),
        authorId = authorId,
        authorName = authorName,
        assignedTo = assignedTo?.let { Uuid.parse(it) },
        assignedToName = assignedToName,
        description = description,
        severity = severity,
        status = status,
        latitude = latitude,
        longitude = longitude,
        accuracyM = accuracyM,
        reportedAt = Instant.parse(reportedAt),
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
        syncStatus = SyncStatus.Synced, // this comes from the API
    )
}

fun Organization.toRequest(): RenameOrganizationRequestDto {
    return RenameOrganizationRequestDto(
        name = name
    )
}

fun Machine.toRequest(): CreateMachineRequestDto {
    return CreateMachineRequestDto(
        organizationId = organizationId.toString(),
        serialNumber = serialNumber,
        model = model
    )
}

fun Incident.toRequest(): CreateIncidentRequestDto {
    return CreateIncidentRequestDto(
        machineId = machineId.toString(),
        description = description,
        severity = severity,
        latitude = latitude,
        longitude = longitude,
        accuracyM = accuracyM,
        reportedAt = reportedAt.toString()
    )
}

fun IncidentUpdate.toRequest(): UpdateIncidentRequestDto {
    return UpdateIncidentRequestDto(
        id = id.toString(),
        message = message,
        newStatus = newStatus,
        assignedTo = assignedTo,
    )
}