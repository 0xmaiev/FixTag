package com.example.fixtag.data.model

enum class IncidentSeverity {
    Low,
    Medium,
    Critical
}

fun incidentSeverityFromString(role: String): IncidentSeverity {
    return when (role) {
        "Baixo" -> {
            IncidentSeverity.Low
        }
        "Medio" -> {
            IncidentSeverity.Medium
        }
        else -> {
            IncidentSeverity.Critical
        }
    }
}
