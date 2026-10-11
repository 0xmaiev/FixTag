package com.example.fixtag.api.model

import com.google.gson.annotations.SerializedName

data class AttachmentDto(
    val id: String,
    @SerializedName("incident_id")
    val incidentId: String,
    val kind: String,
    @SerializedName("content_type")
    val contentType: String,
    @SerializedName("size_bytes")
    val sizeBytes: String,
)
