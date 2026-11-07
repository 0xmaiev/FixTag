package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "machine",
    foreignKeys = [
        ForeignKey(
            entity = Organization::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("organization_id")
        )
    ],
    indices = [Index(value = ["organization_id"])]
)
data class Machine(
    @PrimaryKey
    val id: Uuid,
    @ColumnInfo(name = "organization_id")
    val organizationId: Uuid,
    @ColumnInfo(name = "manufacturer")
    val manufacturerId: Uuid,
    @ColumnInfo(name = "serial_number")
    val serialNumber: String,
    @ColumnInfo(name = "model")
    val model: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "sync_status", defaultValue = "Pending")
    val syncStatus: SyncStatus = SyncStatus.Pending // Default to pending
) {
    companion object {
        fun create(id: Uuid, organizationId: Uuid,
                   manufacturerId: Uuid,
                   serialNumber: String, model: String): Machine {
            return Machine(
                id,
                organizationId,
                manufacturerId,
                serialNumber,
                model,
                Clock.System.now()
            )
        }
    }
}

// a machine plus the counts its list row shows; Room fills the extra columns from the query
data class MachineWithStats(
    @Embedded
    val machine: Machine,
    @ColumnInfo(name = "open_incidents")
    val openIncidents: Int,
    @ColumnInfo(name = "critical_incidents")
    val criticalIncidents: Int,
    @ColumnInfo(name = "recent_incidents")
    val recentIncidents: Int,
)

@Dao
interface MachineDao {
    @Upsert
    suspend fun upsert(machine: Machine)

    @Upsert
    suspend fun upsertAll(machines: List<Machine>)

    @Transaction
    @Query("SELECT * FROM machine WHERE id = :id LIMIT 1")
    fun observe(id: Uuid): Flow<Machine?>

    @Transaction
    @Query("SELECT * FROM machine")
    fun observeAll(): Flow<List<Machine>>

    @Query("""
        SELECT machine.*,
            (SELECT COUNT(*) FROM incident WHERE incident.machine_id = machine.id
                AND incident.status != 'Resolved') AS open_incidents,
            (SELECT COUNT(*) FROM incident WHERE incident.machine_id = machine.id
                AND incident.status != 'Resolved' AND incident.severity = 'Critical') AS critical_incidents,
            (SELECT COUNT(*) FROM incident WHERE incident.machine_id = machine.id
                AND incident.reported_at >= :since) AS recent_incidents
        FROM machine
        WHERE (:client IS NULL OR machine.organization_id = :client)
          AND (:manufacturer IS NULL OR machine.manufacturer = :manufacturer)
        ORDER BY machine.serial_number
    """)
    fun observeVisibleWithStats(client: Uuid?, manufacturer: Uuid?, since: Instant): Flow<List<MachineWithStats>>

    // the same machines without the counts, for the incident form
    @Query("""
        SELECT * FROM machine
         WHERE (:client IS NULL OR organization_id = :client)
           AND (:manufacturer IS NULL OR manufacturer = :manufacturer)
         ORDER BY serial_number
    """)
    fun observeVisible(client: Uuid?, manufacturer: Uuid?): Flow<List<Machine>>

    @Query("SELECT * FROM machine WHERE id = :id")
    suspend fun get(id: Uuid): Machine?

    @Query("SELECT * FROM machine")
    suspend fun getAll(): List<Machine>

    @Query("SELECT * FROM machine WHERE sync_status = 'Pending'")
    suspend fun getPending(): List<Machine>

    @Transaction
    @Query("SELECT * FROM machine WHERE model = :model")
    suspend fun findMachinesByModel(model: String): List<Machine>

    // unique on the server too (409), checked here so the form can say so offline
    @Query("SELECT EXISTS (SELECT 1 FROM machine WHERE serial_number = :serialNumber AND id != :exceptId)")
    suspend fun isSerialNumberTaken(serialNumber: String, exceptId: Uuid): Boolean

    @Query("SELECT COUNT(*) FROM machine WHERE organization_id = :organizationId")
    suspend fun countByOrganization(organizationId: Uuid): Int

    @Delete
    suspend fun delete(machine: Machine)
}