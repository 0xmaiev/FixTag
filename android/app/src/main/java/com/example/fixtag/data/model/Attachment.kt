package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlin.uuid.Uuid

@Entity
data class Attachment(
    @PrimaryKey
    val id: Uuid,
    @ColumnInfo(name = "incident_id")
    val incidentId: Uuid,
    @ColumnInfo(name = "kind")
    val kind: AttachmentKind,
    @ColumnInfo(name = "content_type")
    val contentType: String,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,
    @ColumnInfo(name = "path")
    val path: String,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.Pending // Default to pending
)

@Dao
interface AttachmentDao {
    @Insert
    suspend fun save(attachment: Attachment)

    @Query("SELECT * FROM attachment")
    suspend fun getAll(): List<Attachment>

    @Delete
    suspend fun delete(attachment: Attachment)
}