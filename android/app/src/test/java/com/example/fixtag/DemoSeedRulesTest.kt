package com.example.fixtag

import com.example.fixtag.data.DemoSeed
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.data.model.OrganizationKind
import com.example.fixtag.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock
import kotlin.uuid.Uuid

// What the server's constraints and trigger guarantee for dev.sql, none of which Room checks.
// A plain JVM test, no emulator needed: ./gradlew :app:testDemoDebugUnitTest
class DemoSeedRulesTest {
    private val now = Clock.System.now()
    private val organizations = DemoSeed.organizations(now).associateBy { it.id }
    private val links = DemoSeed.links(now).map { it.clientId to it.manufacturerId }.toSet()
    private val users = DemoSeed.users.associateBy { it.id }
    private val machines = DemoSeed.machines(now).associateBy { it.id }
    private val incidents = DemoSeed.incidents(now)
    private val updates = DemoSeed.incidentUpdates(now)

    @Test
    fun idsAreUnique() {
        // upsert replaces a row with the same id, so a repeated id loses a row without an error
        assertEquals(4, organizations.size)
        assertEquals(13, users.size)
        assertEquals(13, machines.size)
        assertEquals(14, incidents.map { it.id }.toSet().size)
        assertEquals(24, updates.map { it.id }.toSet().size)
    }

    @Test
    fun rolesMatchTheOrganization() {
        for (user in users.values) {
            val roles = when (organizations.getValue(user.organizationId).kind) {
                OrganizationKind.Cliente -> setOf(UserRole.Operador, UserRole.Gestor)
                OrganizationKind.Fabricante ->
                    setOf(UserRole.Assistencia, UserRole.Tecnico, UserRole.Gestor)
            }
            assertTrue("${user.name} can't be ${user.role} there", user.role in roles)
        }
    }

    @Test
    fun machinesAreAtClientsTheirManufacturerServes() {
        for (machine in machines.values) {
            assertTrue(machine.serialNumber, machine.organizationId to machine.manufacturerId in links)
        }
    }

    @Test
    fun incidentsMatchTheirMachineAndHistory() {
        for (incident in incidents) {
            val machine = machines.getValue(incident.machineId)
            assertEquals(machine.organizationId, incident.organizationId)
            assertEquals(machine.manufacturerId, incident.handledBy)
            assertTrue(incident.reportedAt <= incident.createdAt)

            // what the server's incident_updates_after_insert trigger leaves behind
            val history = updates.filter { it.incidentId == incident.id }.sortedBy { it.createdAt }
            assertEquals(
                history.mapNotNull { it.newStatus }.lastOrNull() ?: IncidentStatus.Received,
                incident.status,
            )
            assertEquals(
                history.mapNotNull { it.assignedTo }.lastOrNull(),
                incident.assignedTo?.toString(),
            )
            assertEquals(
                history.mapNotNull { it.assignedToName }.lastOrNull(),
                incident.assignedToName,
            )

            incident.assignedTo?.let {
                val technician = users.getValue(it)
                assertEquals(UserRole.Tecnico, technician.role)
                assertEquals(incident.handledBy, technician.organizationId)
                assertEquals(technician.name, incident.assignedToName)
            }
        }
    }

    @Test
    fun updatesNameTheTechnicianTheyAssign() {
        for (update in updates) {
            val technician = update.assignedTo?.let { users.getValue(Uuid.parse(it)) }
            assertEquals(update.id.toString(), technician?.name, update.assignedToName)
        }
    }
}
