package com.example.fixtag.data.model

enum class IncidentStatus {
    Received,
    Analysis,
    Resolved
}

fun incidentStatusFromString(role: String): IncidentStatus {
    return when (role) {
        "Baixo" -> {
            IncidentStatus.Received
        }
        "Medio" -> {
            IncidentStatus.Analysis
        }
        else -> {
            IncidentStatus.Resolved
        }
    }
}