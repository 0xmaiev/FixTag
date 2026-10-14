package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
//import com.example.fixtag.data.IncidentWithAttachmentsAndUpdates
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity
data class Incident(
    @PrimaryKey
    val id: Uuid,
    @ColumnInfo(name = "organization_id")
    val organizationId: Uuid,
    @ColumnInfo(name = "machine_id")
    val machineId: Uuid,
    @ColumnInfo(name = "author_name")
    val authorName: String,
    val description: String,
    val severity: IncidentSeverity,
    val status: IncidentStatus,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "accuracy_m")
    val accuracyM: Float?,
    @ColumnInfo(name = "reported_at")
    val reportedAt: Instant,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.Pending // Default to pending
)

@Dao
interface IncidentDao {
    @Upsert
    suspend fun save(incident: Incident)

    @Upsert
    suspend fun saveAll(incidents: List<Incident>)

    @Query("SELECT * FROM incident WHERE id = :id LIMIT 1")
    fun observe(id: Uuid): Flow<Incident?>

    @Query("SELECT * FROM incident")
    fun observeAll(): Flow<List<Incident>>

    @Query("SELECT * FROM incident WHERE organization_id = :organizationId")
    fun observeByOrganization(organizationId: Uuid): Flow<List<Incident>>

    @Query("SELECT * FROM incident WHERE machine_id = :machineId")
    fun observeByMachine(machineId: Uuid): Flow<List<Incident>>

    @Query("SELECT * FROM incident")
    suspend fun getAll(): List<Incident>

    @Query("SELECT * FROM incident WHERE sync_status = 'Pending'")
    suspend fun getPending(): List<Incident>

    @Delete
    suspend fun delete(incident: Incident)
}