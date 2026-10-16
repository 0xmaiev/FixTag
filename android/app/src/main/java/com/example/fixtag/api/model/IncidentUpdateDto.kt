package com.example.fixtag.api.model

import com.google.gson.annotations.SerializedName
import kotlin.time.Instant

data class IncidentUpdateDto(
    val id: String,
    @SerializedName("incident_id")
    val incidentId: String,
    @SerializedName("author_id")
    val authorId: String,
    @SerializedName("author_name")
    val authorName: String,
    @SerializedName("message")
    val message: String?,
    @SerializedName("new_status")
    val newStatus: String?,
    @SerializedName("assigned_to")
    val assignedTo: String?,
    @SerializedName("assigned_to_name")
    val assignedToName: String?,
    @SerializedName("created_at")
    val createdAt: String,
)
