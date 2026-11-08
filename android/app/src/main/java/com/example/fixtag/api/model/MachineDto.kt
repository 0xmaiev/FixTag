package com.example.fixtag.api.model

import com.google.gson.annotations.SerializedName

data class MachineDto(
    val id: String,
    @SerializedName("organization_id")
    val organizationId: String,
    @SerializedName("manufacturer_id")
    val manufacturerId: String,
    @SerializedName("serial_number")
    val serialNumber: String,
    val model: String,
    @SerializedName("created_at")
    val createdAt: String,
)
