package com.example.fixtag.api.model

import com.google.gson.annotations.SerializedName

data class IncidentUpdateDto(
    val id: String,
    @SerializedName("author_id")
    val authorId: String,
    @SerializedName("author_name")
    val authorName: String,
    @SerializedName("message")
    val message: String?,
    @SerializedName("new_status")
    val newStatus: String?,
    @SerializedName("created_at")
    val createdAt: Long,
)
