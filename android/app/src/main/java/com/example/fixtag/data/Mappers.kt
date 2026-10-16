package com.example.fixtag.data

import com.example.fixtag.api.CreateIncidentRequestDto
import com.example.fixtag.api.CreateMachineRequestDto
import com.example.fixtag.api.CreateOrganizationRequestDto
import com.example.fixtag.api.UpdateIncidentRequestDto
import com.example.fixtag.api.model.AttachmentDto
import com.example.fixtag.api.model.IncidentDto
import com.example.fixtag.api.model.IncidentUpdateDto
import com.example.fixtag.api.model.MachineDto
import com.example.fixtag.api.model.OrganizationDto
import com.example.fixtag.api.model.TechnicianDto
import com.example.fixtag.data.model.Attachment
import com.example.fixtag.data.model.AttachmentKind
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.data.model.IncidentUpdate
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.OrganizationKind
import com.example.fixtag.data.model.SyncStatus
import com.example.fixtag.data.model.Technician
import kotlin.time.Instant
import kotlin.uuid.Uuid

fun OrganizationDto.toEntity(): Organization {
    return Organization(
        id = Uuid.parse(id),
        name = name,
        kind = OrganizationKind.fromCustomString(kind),
        createdAt = Instant.parse(createdAt),
        syncStatus = SyncStatus.Synced,
    )
}

fun TechnicianDto.toEntity(): Technician {
    return Technician(
        id = Uuid.parse(id),
        organizationId = Uuid.parse(organizationId),
        name = name,
        openIncidents = openIncidents
    )
}

fun MachineDto.toEntity(): Machine {
    return Machine(
        id = Uuid.parse(id),
        organizationId = Uuid.parse(organizationId),
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
        kind = AttachmentKind.fromCustomString(kind),
        contentType = contentType,
        sizeBytes = sizeBytes,
        path = "", // TODO ? need to fetch them
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
        createdAt = Instant.parse(createdAt),
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

fun Organization.toRequest(): CreateOrganizationRequestDto {
    return CreateOrganizationRequestDto(
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
        severity = IncidentSeverity.toCustomString(severity),
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
        newStatus = newStatus
    )
}