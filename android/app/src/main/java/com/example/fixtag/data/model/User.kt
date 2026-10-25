package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
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
    val role: UserRole,
)

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(user: User)

    @Transaction
    @Query("SELECT * FROM user LIMIT 1")
    fun observe(): Flow<User?>

    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun get(id: Uuid): User?

    @Transaction
    @Query("SELECT * FROM user")
    suspend fun getAll(): List<User>

    @Transaction
    @Query("SELECT * FROM user WHERE name = :name")
    suspend fun findUsersByName(name: String): List<User>

    @Delete
    suspend fun delete(user: User)
}