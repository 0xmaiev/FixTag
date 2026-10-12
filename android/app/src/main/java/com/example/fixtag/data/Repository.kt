package com.example.fixtag.data

import android.util.Log
import com.example.fixtag.api.ApiService
import com.example.fixtag.api.LoginRequest
import com.example.fixtag.data.model.Attachment
import com.example.fixtag.data.model.AttachmentDao
import com.example.fixtag.data.model.Auth
import com.example.fixtag.data.model.AuthDao
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentDao
import com.example.fixtag.data.model.IncidentUpdate
import com.example.fixtag.data.model.IncidentUpdateDao
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.MachineDao
import com.example.fixtag.data.model.SyncStatus
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserDao
import com.example.fixtag.data.model.attachmentKindFromString
import com.example.fixtag.data.model.incidentSeverityFromString
import com.example.fixtag.data.model.incidentStatusFromString
import com.example.fixtag.data.model.userRoleFromString
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class Repository(
    private val apiService: ApiService,
    private val authDao: AuthDao,
    private val userDao: UserDao,
    private val machineDao: MachineDao,
    private val incidentDao: IncidentDao,
    private val incidentUpdateDao: IncidentUpdateDao,
    private val attachmentDao: AttachmentDao,
) {

    // in-memory cache of the loggedInUser object
    var user: User? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        user = null
    }

    suspend fun logout() {
        authDao.clear()   // otherwise the next launch restores the session again
        user = null
    }

    suspend fun restoreSession(): User? {
        user?.let { return it }
        val auth = authDao.get() ?: return null
        user = userDao.get(auth.userId)
        return user
    }

    suspend fun login(email: String, password: String): Result<User> {
        try {
            val request = LoginRequest(email, password)
            Log.d("Repository", "Preparing login request")
            // handle login
            val response = apiService.login(request)

            Log.d("Repository", "Got ${response.code()} from /login")
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val userData = body.userDto
                val userId = Uuid.parse(userData.id)
                val orgId = Uuid.parse(userData.organizationId)
                val auth = Auth(
                    userId = userId,
                    refreshToken = body.refreshToken,
                    accessToken = body.accessToken
                )
                authDao.save(auth)
                val user = User(
                    id = userId,
                    organizationId = orgId,
                    name = userData.name,
                    email = userData.email,
                    role = userRoleFromString(userData.role)
                )
                userDao.save(user)
                this.user = user
                return Result.Success(user)
            }

            return Result.Error(Exception("Invalid response body."))

        } catch (e: Exception) {
            Log.d("Repository", e.toString())
            return Result.Error(e)
        }
    }

    suspend fun refreshMachines(): Result<List<Machine>> {
        try {
            Log.d("Repository", "Preparing machines request")
            val response = apiService.getMachines()

            Log.d("Repository", "Got ${response.code()} from /machines")
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val machines = buildList {
                    for ((id, organizationId, serialNumber, model) in body) {
                        add(Machine(
                            Uuid.parse(id),
                            Uuid.parse(organizationId),
                            serialNumber,
                            model
                        ))
                    }
                }
                machineDao.saveAll(machines)
                return Result.Success(machines)
            }

            return Result.Error(Exception("Invalid response body."))

        } catch (e: Exception) {
            Log.d("Repository", e.toString())
            return Result.Error(e)
        }
    }


    suspend fun getIncidents(): Result<List<Incident>> {
        try {
            Log.d("Repository", "Preparing incidents request")
            val response = apiService.getIncidents(limit = 50, null,  null, null, null)

            Log.d("Repository", "Got ${response.code()} from /incidents")
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                val incidents = buildList {
                    for ((id1, organizationId, machineId, _, authorName, description,
                        severity, status, latitude, longitude, accuracyM, reportedAt,
                        createdAt1, updatedAt, attachments1, updates1) in body.items) {

                        val incidentId = Uuid.parse(id1)

                        // build attachments list
                        val attachments = buildList {
                            for ((id, _, kind, contentType, sizeBytes) in attachments1) {
                                val attachment = Attachment(
                                    id = Uuid.parse(id),
                                    incidentId = incidentId,
                                    kind = attachmentKindFromString(kind),
                                    contentType = contentType,
                                    sizeBytes = sizeBytes,
                                    path = "", // TODO ? need to fetch them
                                    syncStatus = SyncStatus.Synced, // this comes from the API
                                )
                                add(attachment)
                            }
                        }

                        // build updates list
                        val updates = buildList {
                            for ((id, authorName, message, newStatus, createdAt) in updates1) {
                                val update = IncidentUpdate(
                                    id = Uuid.parse(id),
                                    incidentId = incidentId,
                                    authorName = authorName,
                                    message = message,
                                    newStatus = newStatus,
                                    syncStatus = SyncStatus.Synced, // this comes from the API
                                )
                                add(update)
                            }
                        }

                        val incident = Incident(
                            id = incidentId,
                            organizationId = Uuid.parse(organizationId),
                            machineId = Uuid.parse(machineId),
                            authorName = authorName,
                            description = description,
                            severity = incidentSeverityFromString(severity),
                            status = incidentStatusFromString(status),
                            latitude = latitude,
                            longitude = longitude,
                            accuracyM = accuracyM,
                            reportedAt = Instant.parse(reportedAt),
                            createdAt = Instant.parse(createdAt1),
                            updatedAt = Instant.parse(updatedAt),
                            syncStatus = SyncStatus.Synced, // this comes from the API
                        )
                        add(incident)

                        // save incident before updates and attachments due to FK and indices
                        incidentDao.save(incident)
                        incidentUpdateDao.saveAll(updates)
                        attachmentDao.saveAll(attachments)
                    }

                }
                return Result.Success(incidents)
            }

            return Result.Error(Exception("Invalid response body."))

        } catch (e: Exception) {
            Log.d("Repository", e.toString())
            return Result.Error(e)
        }
    }

    suspend fun getPendingIncidentUploads(): List<Incident> {
        val pending = incidentDao.getAll()

        return pending
    }

    fun observeMachines(): Flow<List<Machine>> {
        return machineDao.observe()
    }
}