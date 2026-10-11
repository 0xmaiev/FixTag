package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.fixtag.data.UserWithIncidents
import kotlin.uuid.Uuid

@Entity
data class User(
    @PrimaryKey
    val id: Uuid,
    @ColumnInfo(name = "organization_id")
    val organizationId: Uuid,
    val name: String,
    val email: String,
    @ColumnInfo(name = "role")
    val role: String,
)

@Dao
interface UserDao {
    @Upsert
    suspend fun save(user: User)

    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun get(id: Uuid): User?

    @Transaction
    @Query("SELECT * FROM user")
    suspend fun getAll(): List<User>

    @Transaction
    @Query("SELECT * FROM user WHERE name = :name")
    suspend fun findUsersByName(name: String): List<User>

    @Transaction
    @Query("SELECT * FROM user WHERE id = :id")
    abstract fun getIncidentsByUser(id: String): List<UserWithIncidents>

    @Delete
    suspend fun delete(user: User)
}