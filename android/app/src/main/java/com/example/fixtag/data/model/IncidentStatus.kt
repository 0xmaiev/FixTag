package com.example.fixtag.data.model

enum class IncidentStatus {
    Received,
    Analysis,
    Resolved;

    companion object {
        fun fromCustomString(status: String): IncidentStatus {
            return when (status) {
                "recebida" -> {
                    IncidentStatus.Received
                }
                "em_analise" -> {
                    IncidentStatus.Analysis
                }
                "resolvida" -> {
                    IncidentStatus.Resolved
                }
                else -> {
                    throw Exception("invalid incident severity")
                }
            }
        }
        fun toCustomString(status: IncidentStatus): String {
            return when (status) {
                IncidentStatus.Received -> {
                    "recebida"
                }
                IncidentStatus.Analysis -> {
                    "em_analise"
                }
                IncidentStatus.Resolved -> {
                    "resolvida"
                }
            }
        }
    }
}
