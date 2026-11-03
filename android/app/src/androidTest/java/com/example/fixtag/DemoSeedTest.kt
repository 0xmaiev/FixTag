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
import junit.framework.TestCase.assertEquals
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

    @Test
    fun writeDemoSeedData() = runTest {
        val now = Clock.System.now()
        orgDao.upsertAll(DemoSeed.organizations(now))
        linkDao.upsertAll(DemoSeed.links(now))
        userDao.upsertAll(DemoSeed.users)
        machineDao.upsertAll(DemoSeed.machines(now))
        incidentDao.upsertAll(DemoSeed.incidents(now))
        updateDao.upsertAll(DemoSeed.incidentUpdates(now))
    }

}