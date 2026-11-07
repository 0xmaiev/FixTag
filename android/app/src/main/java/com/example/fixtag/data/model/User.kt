package com.example.fixtag.data.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
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

// the roles of the accounts this user can create, empty for everyone else
val User.creatableRoles get() = rolesOf(organizationKind).filter { canCreate(it) }

// what this user can see, each non-null field narrows it
data class Scope(val client: Uuid?, val manufacturer: Uuid?, val technician: Uuid?)

val User.scope get() = when {
    organizationKind == OrganizationKind.Cliente -> Scope(organizationId, null, null)
    role == UserRole.Tecnico -> Scope(null, organizationId, id)
    else -> Scope(null, organizationId, null)
}

// a technician and how many unresolved incidents they hold, for picking who gets the next one
data class TechnicianWithLoad(
    @Embedded
    val user: User,
    @ColumnInfo(name = "open_incidents")
    val openIncidents: Int,
)

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(user: User)

    @Upsert
    suspend fun upsertAll(users: List<User>)

    @Transaction
    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
    fun observe(id: Uuid?): Flow<User?>

    // Room stores the enum's name, 'Tecnico'. incident.assigned_to and user.id are both BLOBs
    @Query("""
        SELECT user.*,
            (SELECT COUNT(*) FROM incident
              WHERE incident.assigned_to = user.id AND incident.status != 'Resolved') AS open_incidents
          FROM user
         WHERE user.role = 'Tecnico' AND user.organization_id = :organizationId
         ORDER BY user.name
    """)
    fun observeTechnicians(organizationId: Uuid): Flow<List<TechnicianWithLoad>>

    @Query("SELECT * FROM user WHERE organization_id = :organizationId ORDER BY name")
    fun observeMembers(organizationId: Uuid): Flow<List<User>>

    // emails are unique on the server too (409), checked here so the form can say so offline
    @Query("SELECT EXISTS (SELECT 1 FROM user WHERE email = :email COLLATE NOCASE)")
    suspend fun isEmailTaken(email: String): Boolean

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