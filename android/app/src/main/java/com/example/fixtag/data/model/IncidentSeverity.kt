package com.example.fixtag.data.model

import com.google.gson.annotations.SerializedName

enum class IncidentSeverity {
    @SerializedName("baixa")
    Low,
    @SerializedName("media")
    Medium,
    @SerializedName("critica")
    Critical;
}
