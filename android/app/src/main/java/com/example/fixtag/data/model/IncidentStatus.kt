package com.example.fixtag.data.model

import com.google.gson.annotations.SerializedName

enum class IncidentStatus {
    @SerializedName("recebida")
    Received,
    @SerializedName("em_analise")
    Analysis,
    @SerializedName("resolvida")
    Resolved;
}
