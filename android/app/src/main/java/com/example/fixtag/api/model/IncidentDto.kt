package com.example.fixtag.api.model

import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.IncidentStatus
import com.google.gson.annotations.SerializedName

data class IncidentDto(
    val id: String,
    @SerializedName("organization_id")
    val organizationId: String,
    @SerializedName("machine_id")
    val machineId: String,
    @SerializedName("author_id")
    val authorId: String,
    @SerializedName("author_name")
    val authorName: String,
    val description: String,
    val severity: IncidentSeverity,
    val status: IncidentStatus,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("accuracy_m")
    val accuracyM: Float?,
    @SerializedName("reported_at")
    val reportedAt: Long,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("updated_at")
    val updatedAt: Long,
    val attachments: List<AttachmentDto>,
    val updates: List<IncidentUpdateDto>,
)
