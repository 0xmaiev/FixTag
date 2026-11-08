package com.example.fixtag

import androidx.room.Room
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fixtag.data.Database
import com.example.fixtag.data.DemoSeed
import com.example.fixtag.data.model.ClientManufacturerLinkDao
import com.example.fixtag.data.model.IncidentDao
import com.example.fixtag.data.model.IncidentUpdateDao
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.MachineDao
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.OrganizationDao
import com.example.fixtag.data.model.OrganizationKind
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserDao
import com.example.fixtag.data.model.UserRole
import com.example.fixtag.data.model.scope
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Clock
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class DemoSeedTest {
    private lateinit var context: Context
    private lateinit var db: Database

    private lateinit var orgDao: OrganizationDao
    private lateinit var linkDao: ClientManufacturerLinkDao
    private lateinit var userDao: UserDao
    private lateinit var machineDao: MachineDao
    private lateinit var incidentDao: IncidentDao
    private lateinit var updateDao: IncidentUpdateDao

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder<Database>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        orgDao = db.getOrganizationDao()
        linkDao = db.getClientManufacturerLinkDao()
        machineDao = db.getMachineDao()
        userDao = db.getUserDao()
        incidentDao = db.getIncidentDao()
        updateDao = db.getIncidentUpdateDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // parents before children, for the foreign keys, as Repository.seedDemoData does
    private suspend fun seed() {
        val now = Clock.System.now()
        orgDao.upsertAll(DemoSeed.organizations(now))
        linkDao.upsertAll(DemoSeed.links(now))
        userDao.upsertAll(DemoSeed.users)
        machineDao.upsertAll(DemoSeed.machines(now))
        incidentDao.upsertAll(DemoSeed.incidents(now))
        updateDao.upsertAll(DemoSeed.incidentUpdates(now))
    }

    @Test
    fun writeDemoSeedData() = runTest {
        seed()
        assertEquals(14, incidentDao.getAll().size)
        assertEquals(24, updateDao.getAll().size)
    }

    // what the server sends each profile for dev.sql: a client its own incidents, a manufacturer's
    // staff the ones it handles, a technician the ones they were ever assigned
    @Test
    fun eachProfileSeesWhatTheServerWouldSend() = runTest {
        seed()
        val expected = mapOf(
            DemoSeed.OPERATOR_1 to Pair(8, 8),     // Ana, Cliente Demo
            DemoSeed.MANAGER_3 to Pair(6, 5),      // João, Metalúrgica Norte: both manufacturers
            DemoSeed.ASSISTANCE_1 to Pair(12, 11), // Carla, everything Fabricante Demo handles
            DemoSeed.ASSISTANCE_2 to Pair(2, 2),   // Luísa, Compressores Atlântico
            DemoSeed.TECHNICIAN_1 to Pair(5, 11),  // Bruno
            DemoSeed.TECHNICIAN_2 to Pair(2, 11),  // Eva
            DemoSeed.TECHNICIAN_3 to Pair(2, 11),  // Filipe
            DemoSeed.TECHNICIAN_4 to Pair(1, 2),   // Marco
        )
        for ((id, counts) in expected) {
            val user = checkNotNull(userDao.get(id))
            val scope = user.scope
            val incidents = incidentDao.observeVisibleWithMachine(
                null, scope.client, scope.manufacturer, scope.technician?.toString()).first()
            val machines = machineDao.observeVisible(scope.client, scope.manufacturer).first()
            assertEquals("${user.name}'s incidents", counts.first, incidents.size)
            assertEquals("${user.name}'s machines", counts.second, machines.size)
        }
    }

    // dev.sql's comment: Bruno has 3 open incidents, Filipe 2, Eva none and Marco 1
    @Test
    fun techniciansCarryTheirOpenIncidents() = runTest {
        seed()
        val fabricanteDemo = userDao.observeTechnicians(DemoSeed.MANUFACTURER_1).first()
            .associate { it.user.name to it.openIncidents }
        assertEquals(
            mapOf(DemoSeed.TECHNICIAN_1_NAME to 3, DemoSeed.TECHNICIAN_2_NAME to 0,
                DemoSeed.TECHNICIAN_3_NAME to 2),
            fabricanteDemo,
        )
        val compressores = userDao.observeTechnicians(DemoSeed.MANUFACTURER_2).first()
        assertEquals(listOf(Pair(DemoSeed.TECHNICIAN_4_NAME, 1)),
            compressores.map { it.user.name to it.openIncidents })
        assertEquals(2, orgDao.observeClients(DemoSeed.MANUFACTURER_1).first().size)
        assertEquals(1, orgDao.observeClients(DemoSeed.MANUFACTURER_2).first().size)
    }

}