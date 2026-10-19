package com.example.fixtag

import androidx.room.Room
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fixtag.data.Database
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserDao
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

    private lateinit var userDao: UserDao

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder<Database>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        userDao = db.getUserDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeUserAndReadInList() = runTest {
        val user = User(Uuid.random(), "Jorge", "Cliente", "Operador")
        userDao.upsert(user)
        val byName = userDao.findUsersByName("Jorge")
        assertEquals(byName.single(), user)
    }
}