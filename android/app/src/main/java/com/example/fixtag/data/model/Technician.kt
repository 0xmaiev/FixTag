package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
//import com.example.fixtag.data.UserWithIncidents
import kotlin.uuid.Uuid

@Entity(
    tableName = "technician",
    foreignKeys = [
        ForeignKey(
            entity = Organization::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("organization_id")
        )
    ],
    indices = [Index(value = ["organization_id"])]
)
data class Technician(
    @PrimaryKey
    val id: Uuid,
    @ColumnInfo(name = "organization_id")
    val organizationId: Uuid,
    val name: String,
    @ColumnInfo(name = "open_incidents")
    val openIncidents: Int,
)

@Dao
interface TechnicianDao {
    @Upsert
    suspend fun save(technician: Technician)

    @Upsert
    suspend fun saveAll(technicians: List<Technician>)

    @Query("SELECT * FROM technician WHERE id = :id")
    suspend fun get(id: Uuid): Technician?

    @Query("SELECT * FROM technician")
    fun observeAll(): Flow<List<Technician>>

    @Transaction
    @Query("SELECT * FROM technician")
    suspend fun getAll(): List<Technician>

    @Transaction
    @Query("SELECT * FROM technician WHERE name = :name")
    suspend fun findTechnicianByName(name: String): List<Technician>

    @Delete
    suspend fun delete(technician: Technician)
}