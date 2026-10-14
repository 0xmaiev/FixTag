package com.example.fixtag.api.model

import com.google.gson.annotations.SerializedName
import kotlin.time.Instant

data class IncidentUpdateDto(
    val id: String,
    @SerializedName("incident_id")
    val incidentId: String,
    @SerializedName("author_name")
    val authorName: String,
    @SerializedName("message")
    val message: String?,
    @SerializedName("new_status")
    val newStatus: String?,
    @SerializedName("created_at")
    val createdAt: String,
)
