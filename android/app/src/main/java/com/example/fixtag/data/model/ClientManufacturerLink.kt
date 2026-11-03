package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Upsert
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = "client_manufacturer_link",
    primaryKeys = ["client_id", "manufacturer_id"],
    foreignKeys = [
        ForeignKey(
            entity = Organization::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE // delete the link if parent is removed
        ),
        ForeignKey(
            entity = Organization::class,
            parentColumns = ["id"],
            childColumns = ["manufacturer_id"],
            onDelete = ForeignKey.CASCADE // delete the link if parent is removed
        )
    ],
    indices = [ // optimizing reverse lookups
        Index(value = ["client_id"]),
        Index(value = ["manufacturer_id"])
    ]
)
data class ClientManufacturerLink(
    @ColumnInfo(name = "client_id")
    val clientId: Uuid,
    @ColumnInfo(name = "manufacturer_id")
    val manufacturerId: Uuid,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "sync_status", defaultValue = "Pending")
    val syncStatus: SyncStatus = SyncStatus.Pending // Default to pending
)

@Dao
interface ClientManufacturerLinkDao {
    @Insert
    suspend fun insert(link: ClientManufacturerLink)

    @Upsert
    suspend fun upsert(link: ClientManufacturerLink)

    @Upsert
    suspend fun upsertAll(links: List<ClientManufacturerLink>)

    @Delete
    suspend fun delete(link: ClientManufacturerLink)
}