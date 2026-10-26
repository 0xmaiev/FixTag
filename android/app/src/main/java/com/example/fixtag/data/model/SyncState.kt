package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

enum class SyncStatus {
    Pending,
    Synced,
    Failed
}

@Entity(
    tableName = "sync_state",
)
data class SyncState(
    @PrimaryKey
    val id: Int = 0,
    @ColumnInfo
    val incidentsCursor: String,
)

@Dao
interface SyncStateDao {
    // Upsert instead of replace because replacing strategy removes before re-inserting,
    // even though there's no issues with FKs or indices this is better in general.
    @Upsert
    suspend fun save(syncState: SyncState)

    @Query("""
        SELECT (SELECT COUNT(*) FROM organization WHERE sync_status = 'Pending')
             + (SELECT COUNT(*) FROM machine WHERE sync_status = 'Pending')
             + (SELECT COUNT(*) FROM incident WHERE sync_status = 'Pending')
             + (SELECT COUNT(*) FROM incident_update WHERE sync_status = 'Pending')
             + (SELECT COUNT(*) FROM attachment WHERE sync_status = 'Pending')
    """)
    fun observePendingCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM sync_state WHERE id = 0 LIMIT 1")
    suspend fun get(): SyncState?

    @Query("DELETE FROM sync_state")
    suspend fun clear()
}