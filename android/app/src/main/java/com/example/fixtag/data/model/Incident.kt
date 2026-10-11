package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.example.fixtag.data.IncidentWithAttachmentsAndUpdates
import kotlin.uuid.Uuid

@Entity
data class Incident(
    @PrimaryKey
    val id: Uuid,
    @ColumnInfo(name = "author_id")
    val authorId: Uuid,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.Pending // Default to pending
)

@Dao
interface IncidentDao {
    @Insert
    suspend fun save(incident: Incident)

    @Query("SELECT * FROM incident")
    suspend fun getAll(): List<Incident>

    @Transaction
    @Query("SELECT * FROM incident WHERE id = :id")
    abstract fun getAttachmentsByIncident(id: Uuid): List<IncidentWithAttachmentsAndUpdates>

    @Delete
    suspend fun delete(incident: Incident)
}