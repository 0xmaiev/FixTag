package com.example.fixtag.data.model

enum class IncidentSeverity {
    Low,
    Medium,
    Critical;

    companion object {
        fun fromCustomString(severity: String): IncidentSeverity {
            return when (severity) {
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
        fun toCustomString(severity: IncidentSeverity): String {
            return when (severity) {
                IncidentSeverity.Low -> {
                    "Baixo"
                }
                IncidentSeverity.Medium -> {
                    "Medio"
                }
                IncidentSeverity.Critical -> {
                    "Critico"
                }
            }
        }
    }
}
