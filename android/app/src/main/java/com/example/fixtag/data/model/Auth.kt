package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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
    // Upsert instead of replace because replacing strategy removes before re-inserting,
    // even though there's no issues with FKs or indices this is better in general.
    @Upsert
    suspend fun upsert(auth: Auth)

    @Transaction
    @Query("SELECT * FROM auth WHERE id = 0 LIMIT 1")
    suspend fun get(): Auth?

    @Query("DELETE FROM auth")
    suspend fun clear()
}