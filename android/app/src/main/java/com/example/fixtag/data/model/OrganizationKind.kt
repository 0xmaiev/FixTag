package com.example.fixtag.data.model

enum class OrganizationKind {
    Client,
    Manufacturer;

    companion object {
        fun fromCustomString(kind: String): OrganizationKind {
            return when (kind) {
                "cliente" -> {
                    OrganizationKind.Client
                }
                "fabricante" -> {
                    OrganizationKind.Manufacturer
                }
                else -> {
                    throw Exception("invalid organization kind")
                }
            }
        }
        fun toCustomString(kind: OrganizationKind): String {
            return when (kind) {
                OrganizationKind.Client -> {
                    "cliente"
                }
                OrganizationKind.Manufacturer -> {
                    "fabricante"
                }
            }
        }
    }
}