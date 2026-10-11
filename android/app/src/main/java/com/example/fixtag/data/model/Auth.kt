package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlin.uuid.Uuid

// The authentication table, stores the user's refresh token
// for access to the API and handles a local relationship between
// the user of the application and other users in the system.
@Entity
data class Auth(
    @PrimaryKey
    val id: Int = 0,
    @ColumnInfo(name = "user_id")
    val userId: Uuid,
    @ColumnInfo(name = "refresh_token")
    val refreshToken: String,
    @ColumnInfo(name = "access_token")
    val accessToken: String,
)

@Dao
interface AuthDao {
    // On conflict, we want to replace the existing row.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(auth: Auth)

    @Transaction
    @Query("SELECT * FROM auth WHERE id = 0 LIMIT 1")
    suspend fun get(): Auth?

    @Query("DELETE FROM auth")
    suspend fun clear()
}