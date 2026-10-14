package com.example.fixtag.data.model

enum class IncidentStatus {
    Received,
    Analysis,
    Resolved;

    companion object {
        fun fromCustomString(status: String): IncidentStatus {
            return when (status) {
                "Recebido" -> {
                    IncidentStatus.Received
                }
                "Analise" -> {
                    IncidentStatus.Analysis
                }
                else -> {
                    IncidentStatus.Resolved
                }
            }
        }
        fun toCustomString(status: IncidentStatus): String {
            return when (status) {
                IncidentStatus.Received -> {
                    "Recebido"
                }
                IncidentStatus.Analysis -> {
                    "Analise"
                }
                else -> {
                    "Resolvido"
                }
            }
        }
    }
}
