package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "incident_update",
    foreignKeys = [
        ForeignKey(
            entity = Incident::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("incident_id")
        )
    ],
    indices = [Index(value = ["incident_id"])]
)
data class IncidentUpdate(
    @PrimaryKey
    val id: Uuid,
    @ColumnInfo(name = "incident_id")
    val incidentId: Uuid,
    @ColumnInfo(name = "author_name")
    val authorName: String,
    @ColumnInfo(name = "message")
    val message: String?,
    @ColumnInfo(name = "new_status")
    val newStatus: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.Pending // Default to pending
){
    companion object {
        fun create(id: Uuid, incidentId: Uuid, authorName: String, message: String?,
                   newStatus: String?): IncidentUpdate {
            return IncidentUpdate(
                id,
                incidentId,
                authorName,
                message,
                newStatus,
                Clock.System.now(),
                SyncStatus.Pending
            )
        }
    }
}

@Dao
interface IncidentUpdateDao {
    @Upsert
    suspend fun save(incidentUpdate: IncidentUpdate)

    @Upsert
    suspend fun saveAll(updates: List<IncidentUpdate>)

    @Transaction
    @Query("SELECT * FROM incident_update WHERE incident_id = :incidentId")
    fun observe(incidentId: Uuid): Flow<List<IncidentUpdate>>

    @Query("SELECT * FROM incident_update")
    suspend fun getAll(): List<IncidentUpdate>

    @Query("SELECT * FROM incident_update WHERE sync_status = 'Pending'")
    suspend fun getPending(): List<IncidentUpdate>

    @Query("SELECT * FROM incident_update WHERE author_name = :authorName")
    suspend fun getIncidentUpdatesByAuthor(authorName: String): List<IncidentUpdate>

    @Delete
    suspend fun delete(incidentUpdate: IncidentUpdate)
}