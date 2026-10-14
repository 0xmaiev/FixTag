package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Entity
data class Machine(
    @PrimaryKey
    val id: Uuid,
    @ColumnInfo(name = "organization_id")
    val organizationId: Uuid,
    @ColumnInfo(name = "serial_number")
    val serialNumber: String,
    @ColumnInfo(name = "role")
    val model: String
)

@Dao
interface MachineDao {
    @Upsert
    suspend fun save(machine: Machine)

    @Upsert
    suspend fun saveAll(machines: List<Machine>)

    @Transaction
    @Query("SELECT * FROM machine WHERE id = :id LIMIT 1")
    fun observe(id: Uuid): Flow<Machine?>

    @Transaction
    @Query("SELECT * FROM machine")
    fun observeAll(): Flow<List<Machine>>

    @Query("SELECT * FROM machine WHERE id = :id")
    suspend fun get(id: Uuid): Machine?

    @Query("SELECT * FROM machine")
    suspend fun getAll(): List<Machine>

    @Delete
    suspend fun delete(machine: Machine)
}