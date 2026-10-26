package com.example.fixtag.api.model

import com.example.fixtag.data.model.OrganizationKind
import com.google.gson.annotations.SerializedName

data class OrganizationDto(
    val id: String,
    val name: String,
    val kind: String,
    @SerializedName("created_at")
    val createdAt: String,
)
