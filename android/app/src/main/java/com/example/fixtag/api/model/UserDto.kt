package com.example.fixtag.api.model

import com.example.fixtag.data.model.OrganizationKind
import com.example.fixtag.data.model.UserRole
import com.google.gson.annotations.SerializedName

data class UserDto(
    val id: String,
    @SerializedName("organization_id")
    val organizationId: String,
    @SerializedName("organization_kind")
    val organizationKind: OrganizationKind,
    val name: String,
    val email: String,
    @SerializedName("must_change_password")
    val mustChangePassword: Boolean,
    val role: UserRole,
    @SerializedName("created_at")
    val createdAt: String,
)
