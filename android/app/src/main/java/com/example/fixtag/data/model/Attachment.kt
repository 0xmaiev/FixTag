package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid


@Entity(
    tableName = "attachment",
    foreignKeys = [
        ForeignKey(
            entity = Incident::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("incident_id")
        )
    ],
    indices = [Index(value = ["incident_id"])]
)
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
    val sizeBytes: Int,
    @ColumnInfo(name = "path")
    var path: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    // syncStatus means whether this was synced TO API or not
    @ColumnInfo(name = "sync_status")
    var syncStatus: SyncStatus = SyncStatus.Pending, // Default to pending
    // fetchStatus means whether we fetched the actual data blob FROM API or not
    @ColumnInfo(name = "fetch_status")
    var fetchStatus: SyncStatus = SyncStatus.Pending // Default to pending
) {
    companion object {
        fun create(id: Uuid, incidentId: Uuid, kind: AttachmentKind, contentType: String,
                   sizeBytes: Int, path: String?): Attachment {
            return Attachment(
                id,
                incidentId,
                kind,
                contentType,
                sizeBytes,
                path,
                Clock.System.now()
            )
        }
    }
}

@Dao
interface AttachmentDao {
    // attachments don't change once they exist
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(attachment: Attachment)

    // attachments don't change once they exist - this method
    // is used specifically when syncing FROM the server
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(attachments: List<Attachment>)

    @Update
    suspend fun update(attachment: Attachment)

    @Update
    suspend fun updateAll(attachment: List<Attachment>)

    @Transaction
    @Query("SELECT * FROM attachment WHERE incident_id = :incidentId")
    fun observe(incidentId: Uuid): Flow<List<Attachment>>

    @Query("SELECT * FROM attachment WHERE incident_id = :incidentId")
    suspend fun getByIncident(incidentId: Uuid): List<Attachment>

    @Query("SELECT * FROM attachment")
    suspend fun getAll(): List<Attachment>

    @Query("SELECT * FROM attachment WHERE sync_status = 'Pending'")
    suspend fun getPending(): List<Attachment>

    @Query("SELECT * FROM attachment WHERE fetch_status = 'Pending'")
    suspend fun getPendingFetch(): List<Attachment>

    @Delete
    suspend fun delete(attachment: Attachment)

    @Delete
    suspend fun deleteAll(attachments: List<Attachment>)
}