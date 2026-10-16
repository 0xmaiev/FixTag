package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Instant
//import com.example.fixtag.data.UserWithIncidents
import kotlin.uuid.Uuid

@Entity
data class Organization(
    @PrimaryKey
    val id: Uuid,
    val name: String,
    val kind: OrganizationKind,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.Pending // Default to pending
) {
    companion object {
        fun create(id: Uuid, name: String): Organization {
            return Organization(
                id,
                name,
                OrganizationKind.Client, // only clients get created for now
                Clock.System.now()
            )
        }
    }
}


@Dao
interface OrganizationDao {
    @Insert
    suspend fun insert(organization: Organization)

    @Upsert
    suspend fun upsert(organization: Organization)

    @Upsert
    suspend fun upsertAll(organizations: List<Organization>)

    @Query("SELECT * FROM organization WHERE id = :id")
    suspend fun get(id: Uuid): Organization?

    @Query("SELECT * FROM organization")
    fun observeAll(): Flow<List<Organization>>

    @Transaction
    @Query("SELECT * FROM organization")
    suspend fun getAll(): List<Organization>

    @Query("SELECT * FROM organization WHERE sync_status = 'Pending'")
    suspend fun getPending(): List<Organization>

    @Transaction
    @Query("SELECT * FROM organization WHERE name = :name")
    suspend fun findOrganizationsByName(name: String): List<Organization>

    @Delete
    suspend fun delete(organization: Organization)
}