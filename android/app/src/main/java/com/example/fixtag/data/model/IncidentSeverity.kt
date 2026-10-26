package com.example.fixtag.data.model

enum class IncidentSeverity {
    Low,
    Medium,
    Critical;

    companion object {
        fun fromCustomString(severity: String): IncidentSeverity {
            return when (severity) {
                "baixa" -> {
                    IncidentSeverity.Low
                }
                "media" -> {
                    IncidentSeverity.Medium
                }
                "critica" -> {
                    IncidentSeverity.Critical
                }
                else -> {
                    throw Exception("invalid incident severity")
                }
            }
        }
        fun toCustomString(severity: IncidentSeverity): String {
            return when (severity) {
                IncidentSeverity.Low -> {
                    "baixa"
                }
                IncidentSeverity.Medium -> {
                    "media"
                }
                IncidentSeverity.Critical -> {
                    "critica"
                }
            }
        }
    }
}
