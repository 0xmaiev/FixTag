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
    @ColumnInfo(name = "organization_kind")
    val organizationKind: OrganizationKind,
    val name: String,
    val email: String,
    @ColumnInfo(name = "role")
    val role: UserRole,
    @ColumnInfo(name = "must_change_password", defaultValue = "false")
    val mustChangePassword: Boolean = false,
    @ColumnInfo(name = "sync_status", defaultValue = "Pending")
    val syncStatus: SyncStatus = SyncStatus.Pending // Default to pending
)  {
    companion object {
        fun create(id: Uuid, organizationId: Uuid, organizationKind: OrganizationKind,
                   name: String, email: String, role: UserRole): User {
            return User(
                id,
                organizationId,
                organizationKind,
                name,
                email,
                role,
                mustChangePassword = true,
                syncStatus = SyncStatus.Pending
            )
        }
    }
}


fun rolesOf(kind: OrganizationKind) = when (kind) {
    OrganizationKind.Cliente -> listOf(UserRole.Operador, UserRole.Gestor)
    OrganizationKind.Fabricante -> listOf(UserRole.Assistencia, UserRole.Tecnico, UserRole.Gestor)
}

// triages incidents, assigns technicians, registers clients and machines
val User.isSupport get() =
    organizationKind == OrganizationKind.Fabricante &&
            (role == UserRole.Assistencia || role == UserRole.Gestor)

// accounts are always created in the creator's own organization
fun User.canCreate(role: UserRole) = when (this.role) {
    UserRole.Gestor -> role in rolesOf(organizationKind)
    UserRole.Assistencia -> role == UserRole.Tecnico
    else -> false
}

// what this user can see, each non-null field narrows it
data class Scope(val client: Uuid?, val manufacturer: Uuid?, val technician: Uuid?)

val User.scope get() = when {
    organizationKind == OrganizationKind.Cliente -> Scope(organizationId, null, null)
    role == UserRole.Tecnico -> Scope(null, organizationId, id)
    else -> Scope(null, organizationId, null)
}

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(user: User)

    @Upsert
    suspend fun upsertAll(users: List<User>)

    @Transaction
    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
    fun observe(id: Uuid?): Flow<User?>

    @Transaction
    @Query("SELECT * FROM user WHERE role = 'Technician'")
    fun observeTechnicians(): Flow<List<User>>

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