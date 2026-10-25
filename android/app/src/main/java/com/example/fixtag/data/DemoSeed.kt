package com.example.fixtag.data

import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.OrganizationKind
import com.example.fixtag.data.model.SyncStatus
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserRole
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.Uuid

// Demo variant only: what the server would have sent. Same ids as backend/seeds/dev.sql.
object DemoSeed {
    val CLIENT = Uuid.parse("a0000000-0000-4000-8000-000000000001")
    val MANUFACTURER = Uuid.parse("a0000000-0000-4000-8000-000000000002")
    val OPERATOR = Uuid.parse("b0000000-0000-4000-8000-000000000001")
    val ASSISTANCE = Uuid.parse("b0000000-0000-4000-8000-000000000003")
    private val PRESS = Uuid.parse("c0000000-0000-4000-8000-000000000001")
    private val LATHE = Uuid.parse("c0000000-0000-4000-8000-000000000002")
    private val COMPRESSOR = Uuid.parse("c0000000-0000-4000-8000-000000000003")

    fun organizations(now: Instant) = listOf(
        Organization(CLIENT, "Cliente Demo, Lda.", OrganizationKind.Client, now, SyncStatus.Synced),
        Organization(
            MANUFACTURER,
            "Fabricante Demo, S.A.",
            OrganizationKind.Manufacturer,
            now,
            SyncStatus.Synced
        ),
    )

    val users = listOf(
        User(OPERATOR, CLIENT, "Ana Operadora", "operador@fixtag.test", UserRole.Operator),
        User(
            ASSISTANCE,
            MANUFACTURER,
            "Carla Assistência",
            "assistencia@fixtag.test",
            UserRole.Assistance
        ),
    )

    fun machines(now: Instant) = listOf(
        Machine(PRESS, CLIENT, "FT-0001", "Prensa hidráulica PH-200", now, SyncStatus.Synced),
        Machine(LATHE, CLIENT, "FT-0002", "Torno CNC TC-450", now, SyncStatus.Synced),
        Machine(COMPRESSOR, CLIENT, "FT-0003", "Compressor CP-90", now, SyncStatus.Synced),
    )

    // reports already "sent", so the list shows synced ones next to the pending ones you create
    fun incidents(now: Instant) = listOf(
        Incident(
            Uuid.parse("d0000000-0000-4000-8000-000000000001"), CLIENT, PRESS, "Ana Operadora",
            "Fuga de óleo no cilindro principal", IncidentSeverity.Critical, 41.1579, -8.6291, 12f,
            now - 2.days, now - 2.days, now - 2.days, IncidentStatus.Analysis, SyncStatus.Synced
        ),
        Incident(Uuid.parse("d0000000-0000-4000-8000-000000000002"), CLIENT, LATHE, "Diogo Operador",
            "Ruído anormal no eixo durante o arranque", IncidentSeverity.Medium, null, null, null,
            now - 1.days, now - 1.days, now - 1.days, IncidentStatus.Received, SyncStatus.Synced),
    )
}