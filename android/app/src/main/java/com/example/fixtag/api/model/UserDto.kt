package com.example.fixtag.api.model

import com.google.gson.annotations.SerializedName

data class UserDto(
    val id: String,
    @SerializedName("organization_id")
    val organizationId: String,
    val name: String,
    val email: String,
    val role: String,
)
