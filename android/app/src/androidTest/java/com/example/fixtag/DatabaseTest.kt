package com.example.fixtag

import androidx.room.Room
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fixtag.data.Database
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
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var context: Context
    private lateinit var db: Database

    private lateinit var orgDao: OrganizationDao
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
        machineDao = db.getMachineDao()
        userDao = db.getUserDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeOrganization() = runTest {
        val org = Organization.create(
            Uuid.random(),
            "Test Org",
        )
        orgDao.upsert(org)
        val byName = orgDao.findOrganizationsByName("Test Org")
        val single = byName.single()
        assertEquals(org.id, single.id)
        assertEquals(org.name, single.name)
        assertEquals(org.syncStatus, single.syncStatus)
    }

    @Test
    fun writeMachine() = runTest {
        val org = orgDao.findOrganizationsByName("Test Org").single()
        val machine = Machine.create(
            Uuid.random(),
            org.id,
            "FT-0001",
            "Prensa"
        )
        machineDao.upsert(machine)
        val byName = machineDao.findMachinesByModel("Prensa")
        assertEquals(machine, byName.single() )
    }

    @Test
    fun writeUserAndReadInList() = runTest {
        val user = User(
            Uuid.random(),
            Uuid.random(),
            "Jorge",
            "jorge@fixtag.test",
            UserRole.Operator
        )
        userDao.upsert(user)
        val byName = userDao.findUsersByName("Jorge")
        assertEquals(user, byName.single())
    }
}