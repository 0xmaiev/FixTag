package com.example.fixtag.data.model

enum class UserRole {
    Operator,
    Technician,
    Assistance;

    companion object {
        fun fromCustomString(role: String): UserRole {
            return when (role) {
                "operador" -> {
                    UserRole.Operator
                }
                "tecnico" -> {
                    UserRole.Technician
                }
                "assistencia" -> {
                    UserRole.Assistance
                }
                else -> {
                    throw Exception("invalid user role")
                }
            }
        }
        fun toCustomString(role: UserRole): String {
            return when (role) {
                UserRole.Operator -> {
                    "operador"
                }
                UserRole.Technician -> {
                    "tecnico"
                }
                UserRole.Assistance -> {
                    "assistencia"
                }
            }
        }
    }
}
