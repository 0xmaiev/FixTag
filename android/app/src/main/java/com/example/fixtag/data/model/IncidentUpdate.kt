package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlin.uuid.Uuid

@Entity(
    tableName = "incident_update",
    foreignKeys = [
        ForeignKey(
            entity = Incident::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("incident_id")
        )
    ]
)
data class IncidentUpdate(
    @PrimaryKey
    val id: Uuid,
    @ColumnInfo(name = "incident_id")
    val incidentId: Uuid,
    @ColumnInfo(name = "author_name")
    val authorName: Uuid,
    @ColumnInfo(name = "message")
    val message: String?,
    @ColumnInfo(name = "new_status")
    val newStatus: String?,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.Pending // Default to pending
)

@Dao
interface IncidentUpdateDao {
    @Insert
    suspend fun save(incidentUpdate: IncidentUpdate)

    @Query("SELECT * FROM incident_update")
    suspend fun getAll(): List<IncidentUpdate>

    @Query("SELECT * FROM incident_update WHERE author_name = :authorName")
    suspend fun getIncidentUpdatesByAuthor(authorName: String): List<IncidentUpdate>

    @Delete
    suspend fun delete(incidentUpdate: IncidentUpdate)
}