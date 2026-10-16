package com.example.fixtag.api.model

import com.google.gson.annotations.SerializedName

data class TechnicianDto(
    val id: String,
    @SerializedName("organization_id")
    val organizationId: String,
    val name: String,
    @SerializedName("open_incidents")
    val openIncidents: Int
)
