package com.example.fixtag.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fixtag.data.model.Attachment
import com.example.fixtag.data.model.AttachmentDao
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentDao
import com.example.fixtag.data.model.IncidentUpdate
import com.example.fixtag.data.model.IncidentUpdateDao
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.MachineDao
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserDao
import com.example.fixtag.data.model.Auth
import com.example.fixtag.data.model.AuthDao
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.OrganizationDao
import com.example.fixtag.data.model.SyncState
import com.example.fixtag.data.model.SyncStateDao
import com.example.fixtag.data.model.Technician
import com.example.fixtag.data.model.TechnicianDao

const val DATABASE_NAME = "database"

@Database(version = 2, entities = [
    Auth::class,
    Organization::class,
    User::class,
    Technician::class,
    Incident::class,
    IncidentUpdate::class,
    Attachment::class,
    Machine::class,
    SyncState::class,
], exportSchema = false)
@TypeConverters(UuidConverter::class, InstantConverter::class)
abstract class Database : RoomDatabase() {
    companion object {
        @Volatile
        private var INSTANCE: com.example.fixtag.data.Database? = null

        fun getDatabase(context: Context): com.example.fixtag.data.Database {
            // Return the existing instance if available, or build a new one
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder<com.example.fixtag.data.Database>(
                    context.applicationContext,
                    DATABASE_NAME,
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }

    // AuthDao is a class annotated with @Dao.
    abstract fun getAuthDao(): AuthDao

    // UserDao is a class annotated with @Dao.
    abstract fun getUserDao(): UserDao

    // OrganizationDao is a class annotated with @Dao.
    abstract fun getOrganizationDao(): OrganizationDao

    // TechnicianDao is a class annotated with @Dao.
    abstract fun getTechnicianDao(): TechnicianDao

    // MachineDao is a class annotated with @Dao.
    abstract fun getMachineDao(): MachineDao

    // IncidentDao is a class annotated with @Dao.
    abstract fun getIncidentDao(): IncidentDao

    // IncidentUpdateDao is a class annotated with @Dao.
    abstract fun getIncidentUpdateDao(): IncidentUpdateDao

    // AttachmentDao is a class annotated with @Dao.
    abstract fun getAttachmentDao(): AttachmentDao

    // SyncStateDao is a class annotated with @Dao.
    abstract fun getSyncStateDao(): SyncStateDao
}
