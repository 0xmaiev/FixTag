package com.example.fixtag.data.model

enum class UserRole {
    Operator,
    Technician,
    Assistance
}

fun userRoleFromString(role: String): UserRole {
    return when (role) {
        "operador" -> {
            UserRole.Operator
        }
        "tecnico" -> {
            UserRole.Technician
        }
        else -> {
            UserRole.Assistance
        }
    }
}

