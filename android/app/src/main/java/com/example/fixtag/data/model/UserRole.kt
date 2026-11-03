package com.example.fixtag.data.model

import com.google.gson.annotations.SerializedName

// -------------------------------------------------------------------------------------
// Simple Platform Overview
// -------------------------------------------------------------------------------------

// -------------------------------------------------------------------------------------
// Platform Management Case
// -------------------------------------------------------------------------------------

// Platform Admins can create Manufacturer Organizations and add a designated
// Manager account for that Organization.
// Admins have access to platform wide data visualization related to
// the platform's operation as a whole, not necessarily to each
// Organization's operation.

// -------------------------------------------------------------------------------------
// Account Management
// -------------------------------------------------------------------------------------

// All accounts for a <organization>-<role> have a predefined password
// for sake of smaller scope since it's a "closed" platform that can
// be used by interested Organizations (Manufacturers), not a platform
// with an open registration form.
// It is however possible to change an account's password.
// Sending emails or another mechanism for account verification is left out
// of the scope of this project for the time being.

// -------------------------------------------------------------------------------------
// Platform User Case
// -------------------------------------------------------------------------------------

// -------------------------------------------------------------------------------------
// Manufacturer Organizations
// -------------------------------------------------------------------------------------

// Organization Managers have the ability to add Assistance accounts to an
// organization and access to data visualization related to the Organization,
// and it's relationship with its customers.

// Assistance accounts (akin to non-technical customer support) are first
// responders to Incidents, they are the ones who "manage" the Organization's
// Technicians and assign them to Incidents.

// Technicians get assigned Incidents by any Assistance account from their own
// organization, they only have access to their assigned Incidents, whether
// they have been closed or not.

// -------------------------------------------------------------------------------------
// Client Organizations
// -------------------------------------------------------------------------------------

// Operators can only create Incidents and see those related to their
// Organization's Machines.

enum class UserRole {
    // platform admin account
    @SerializedName("admin")
    Admin,
    // client organization machine operator
    @SerializedName("operador")
    Operador,
    // manufacturer technical support that performs site visits

    @SerializedName("tecnico")
    Tecnico,
    // this is more akin of a non-technical customer support role
    @SerializedName("assistencia")
    Assistencia,
    // organization manager, high level data view, adds assistance
    // accounts to organization
    @SerializedName("gestor")
    Gestor;

    companion object {
        fun fromCustomString(role: String): UserRole {
            return when (role) {
                "admin" -> {
                    UserRole.Admin
                }
                "operador" -> {
                    UserRole.Operador
                }
                "tecnico" -> {
                    UserRole.Tecnico
                }
                "assistencia" -> {
                    UserRole.Assistencia
                }
                "gestor" -> {
                    UserRole.Gestor
                }
                else -> {
                    throw Exception("invalid user role")
                }
            }
        }
        fun toCustomString(role: UserRole): String {
            return when (role) {
                UserRole.Admin -> {
                    "admin"
                }
                UserRole.Operador -> {
                    "operador"
                }
                UserRole.Tecnico -> {
                    "tecnico"
                }
                UserRole.Assistencia -> {
                    "assistencia"
                }
                UserRole.Gestor -> {
                    "gestor"
                }
            }
        }
    }
}
