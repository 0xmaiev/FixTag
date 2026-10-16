package com.example.fixtag.data

import android.util.Log
import androidx.room.withTransaction
import com.example.fixtag.api.ApiService
import com.example.fixtag.api.LoginRequestDto
import com.example.fixtag.data.model.Attachment
import com.example.fixtag.data.model.Auth
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentAndMachine
import com.example.fixtag.data.model.IncidentUpdate
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.SyncState
import com.example.fixtag.data.model.SyncStatus
import com.example.fixtag.data.model.Technician
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.BufferedInputStream
import java.io.File
import kotlin.uuid.Uuid

// Directory for local attachment storage
const val FILE_DIR: String = "filesDir/attachments/"

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class Repository(
    private val db: Database,
    private val syncScheduler: SyncScheduler,
    private val apiService: ApiService,
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
        db.getAuthDao().clear()   // otherwise the next launch restores the session again
        user = null
    }

    suspend fun restoreSession(): User? {
        user?.let { return it }
        val authDao = db.getAuthDao()
        val auth = authDao.get() ?: return null
        val userDao = db.getUserDao()
        user = userDao.get(auth.userId)
        return user
    }

    suspend fun login(email: String, password: String): Result<User> {
        try {
            val request = LoginRequestDto(email, password)
            Log.d("Repository", "Preparing login request")
            // handle login
            val response = apiService.login(request)

            Log.d("Repository", "Got ${response.code()} from /login")
            if (response.isSuccessful && response.body() != null) {
                val authDao = db.getAuthDao()
                val userDao = db.getUserDao()
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
                    role = UserRole.fromCustomString(userData.role)
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

    suspend fun refreshTechnicians(): Result<List<Technician>> {
        try {
            Log.d("Repository", "Preparing technicians request")
            val response = apiService.getTechnicians()

            Log.d("Repository", "Got ${response.code()} from /technicians")
            if (response.isSuccessful && response.body() != null) {
                val technicianDao = db.getTechnicianDao()
                val body = response.body()!!
                val technicians = body.map { it.toEntity() }
                technicianDao.saveAll(technicians)
                return Result.Success(technicians)
            }

            return Result.Error(Exception("Invalid response body."))

        } catch (e: Exception) {
            Log.d("Repository", e.toString())
            return Result.Error(e)
        }
    }


    suspend fun refreshOrganizations(): Result<List<Organization>> {
        try {
            Log.d("Repository", "Preparing organizations request")
            val response = apiService.getOrganizations()

            Log.d("Repository", "Got ${response.code()} from /organizations")
            if (response.isSuccessful && response.body() != null) {
                val organizationDao = db.getOrganizationDao()
                val body = response.body()!!
                val organizations = body.map { it.toEntity() }
                organizationDao.upsertAll(organizations)
                return Result.Success(organizations)
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
                val machineDao = db.getMachineDao()
                val body = response.body()!!
                val machines = body.map { it.toEntity() }
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
                val incidentDao = db.getIncidentDao()
                val incidentUpdateDao = db.getIncidentUpdateDao()
                val attachmentDao = db.getAttachmentDao()
                val body = response.body()!!

                val incidents = buildList {
                    for (incidentDto in body.items) {

                        val incidentId = Uuid.parse(incidentDto.id)

                        // build attachments list
                        val attachments = buildList {
                            for (attachmentDto in incidentDto.attachments) {
                                val attachment = attachmentDto.toEntity(incidentId)
                                add(attachment)
                            }
                        }

                        // build updates list
                        val updates = buildList {
                            for (updateDto in incidentDto.updates) {
                                val update = updateDto.toEntity(incidentId)
                                add(update)
                            }
                        }

                        val incident = incidentDto.toEntity(incidentId)
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

    suspend fun syncIncidents() {
        val syncStateDao = db.getSyncStateDao()
        val incidentDao = db.getIncidentDao()
        val incidentUpdateDao = db.getIncidentUpdateDao()
        val attachmentDao = db.getAttachmentDao()
        var cursor = syncStateDao.get()?.incidentsCursor

        Log.i("Repository", "Synchronising incidents")

        do {
            val response = apiService.getIncidents(limit = 100, cursor = cursor, null, null, null)
            val page = response.body() ?: throw HttpException(response)

            Log.d("Repository", "Got ${page.items} incidents")

            db.withTransaction {
                for (dto in page.items) {
                    val incidentId = Uuid.parse(dto.id)
                    // save incident before updates and attachments due to FK and indices
                    incidentDao.save(dto.toEntity(incidentId))
                    incidentUpdateDao.saveAll(dto.updates.map { it.toEntity(incidentId) })
                    attachmentDao.saveAll(dto.attachments.map { it.toEntity(incidentId)})
                }
                cursor = page.nextCursor
                if (cursor != null) {
                    syncStateDao.save(SyncState(incidentsCursor = cursor!!))
                }
            }
        } while (page.hasMore)

    }

    // organizations -> machines -> incidents -> attachments / updates (server FK order)
    suspend fun pushPending() {
        pushPendingOrganizations()
        pushPendingMachines()
        pushPendingIncidents()
        pushPendingUpdates()
        pushPendingAttachments()
    }

    // technicians -> organizations  -> incidents -> attachments / updates (server FK order)
    suspend fun pullMissing() {
        refreshOrganizations()
        refreshTechnicians()
        refreshMachines()
        syncIncidents()
        downloadAttachments()
    }

    suspend fun pushPendingOrganizations() {
        val organizationDao = db.getOrganizationDao()
        val pendingOrganizations = organizationDao.getPending()

        if (pendingOrganizations.isEmpty()) {
            Log.i("Repository", "No pending organizations to push")
            return
        }

        Log.i("Repository", "Pushing  ${pendingOrganizations.size} pending organizations")

        db.withTransaction {
            for (organization in pendingOrganizations) {
                val request = organization.toRequest()
                val response = apiService.createOrganization(organization.id.toString(), request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    organizationDao.upsert(body.toEntity())
                    Log.i("Repository", "Successfully pushed organizations")
                }
            }
        }
    }

    suspend fun pushPendingMachines() {
        val machineDao = db.getMachineDao()
        val pendingMachines = machineDao.getPending()

        if (pendingMachines.isEmpty()) {
            Log.i("Repository", "No pending machines to push")
            return
        }

        Log.i("Repository", "Pushing  ${pendingMachines.size} pending machines")

        db.withTransaction {
            for (machine in pendingMachines) {
                val request = machine.toRequest()
                val response = apiService.createMachine(machine.id.toString(), request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    machineDao.save(body.toEntity())
                    Log.i("Repository", "Successfully pushed machines")
                }
            }
        }
    }

    suspend fun pushPendingIncidents() {
        val incidentDao = db.getIncidentDao()
        val pendingIncidents = incidentDao.getPending()

        if (pendingIncidents.isEmpty()) {
            Log.i("Repository", "No pending incidents to push")
            return
        }

        Log.i("Repository", "Pushing  ${pendingIncidents.size} pending incidents")

        db.withTransaction {
            for (incident in pendingIncidents) {
                val request = incident.toRequest()
                val response = apiService.createIncident(incident.id.toString(), request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    incidentDao.save(body.toEntity(incident.id))
                    Log.i("Repository", "Successfully pushed incident")
                }
            }
        }
    }

    suspend fun pushPendingUpdates() {
        val incidentDao = db.getIncidentDao()
        val updateDao = db.getIncidentUpdateDao()
        val pendingUpdates = updateDao.getPending()

        if (pendingUpdates.isEmpty()) {
            Log.i("Repository", "No pending incident updates to push")
            return
        }

        Log.i("Repository", "Pushing ${pendingUpdates.size} pending incident updates")

        db.withTransaction {
            for (update in pendingUpdates) {
                val request = update.toRequest()
                val response = apiService.addIncidentUpdate(update.incidentId.toString(), request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val updatedIncident = body.toEntity(update.incidentId)
                    incidentDao.save(updatedIncident)
                    val syncedUpdate = body.updates.find { value -> value.id == update.id.toString() }
                    if (syncedUpdate != null) {
                        updateDao.save(syncedUpdate.toEntity(update.incidentId))
                        Log.i("Repository", "Successfully pushed incident update")
                    }
                }
            }
        }
    }

    suspend fun pushPendingAttachments() {
        val attachmentDao = db.getAttachmentDao()
        val pendingAttachments = attachmentDao.getPending()

        if (pendingAttachments.isEmpty()) {
            Log.i("Repository", "No pending attachments to push")
            return
        }

        Log.i("Repository", "Pushing  ${pendingAttachments.size} pending attachments")

        for (attachment in pendingAttachments) {
            // the path is always going to be non-null when it's pending sync
            val attachmentFile = File(attachment.path!!)
            // but we can check if the file actually exists beforehand
            // and if not, we remove the attachment entirely
            if (attachmentFile.exists()){
                val request = attachmentFile.asRequestBody(attachment.contentType.toMediaType())
                val response = apiService.addAttachment(
                    attachment.incidentId.toString(),
                    attachment.id.toString(),
                    request
                )
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val syncedAttachment = body.toEntity(
                        attachment.incidentId,
                    )
                    syncedAttachment.path = attachment.path
                    syncedAttachment.fetchStatus = SyncStatus.Synced
                    attachmentDao.save(syncedAttachment)
                    Log.i("Repository", "Successfully pushed attachment")
                } else {
                    attachment.syncStatus = SyncStatus.Failed
                    attachmentDao.update(attachment)
                }
            } else {
                attachmentDao.delete(attachment)
                Log.i("Repository", "Removed attachment with non-existing file")
            }
        }
    }

    suspend fun downloadAttachments() {
        val attachmentDao = db.getAttachmentDao()
        val pendingAttachments = attachmentDao.getPendingFetch()

        for (attachment in pendingAttachments) {
            val filePath = withContext(Dispatchers.IO) {
                val response = apiService.downloadAttachment(attachment.id.toString())
                val body = response.body()

                if (response.isSuccessful && body != null) {
                    val fileName = attachment.id.toString().slice(0..8)
                    streamToFile(body.byteStream().buffered(), fileName)
                    fileName
                } else {
                    null
                }
            }
            // update attachment
            attachment.path = filePath
            attachment.fetchStatus = SyncStatus.Synced
            attachmentDao.update(attachment)
        }

    }

    fun observeOrganizations(): Flow<List<Organization>> {
        return db.getOrganizationDao().observeAll()
    }

    fun observeTechnicians(): Flow<List<Technician>> {
        return db.getTechnicianDao().observeAll()
    }

    fun observeIncidents(): Flow<List<Incident>> {
        return db.getIncidentDao().observeAll()
    }

    fun observeIncident(id: Uuid): Flow<Incident?> {
        return db.getIncidentDao().observe(id)
    }

    fun observeIncidentAndMachine(id: Uuid): Flow<IncidentAndMachine> {
        return db.getIncidentDao().observeWithMachine(id)
    }

    fun observeIncidentsByMachine(machineId: Uuid): Flow<List<Incident>> {
        return db.getIncidentDao().observeByMachine(machineId)
    }

    fun observeIncidentUpdates(incidentId: Uuid): Flow<List<IncidentUpdate>> {
        return db.getIncidentUpdateDao().observe(incidentId)
    }

    fun observeAttachments(incidentId: Uuid): Flow<List<Attachment>> {
        return db.getAttachmentDao().observe(incidentId)
    }

    fun observeMachine(id: Uuid): Flow<Machine?> {
        return db.getMachineDao().observe(id)
    }

    fun observeMachines(): Flow<List<Machine>> {
        return db.getMachineDao().observeAll()
    }

    suspend fun createOrganization(organization: Organization): Organization {
        val organizationDao = db.getOrganizationDao()
        organizationDao.upsert(organization)

        syncScheduler.syncNow()
        return organization
    }

    suspend fun createMachine(machine: Machine) {
        val machineDao = db.getMachineDao()
        machineDao.save(machine)

        syncScheduler.syncNow()
    }

    suspend fun createIncident(incident: Incident) {
        val incidentDao = db.getIncidentDao()
        incidentDao.save(incident)

        syncScheduler.syncNow()
    }

    suspend fun createIncidentUpdate(incidentUpdate: IncidentUpdate) {
        val incidentUpdateDao = db.getIncidentUpdateDao()
        incidentUpdateDao.save(incidentUpdate)

        syncScheduler.syncNow()
    }

}

fun streamToFile(inputStream: BufferedInputStream, fileName: String) {
    val fileDir = File(FILE_DIR)
    val tmpFile = File.createTempFile(
        "tmp",
        fileName,
        fileDir
    )
    var offset = 0
    inputStream.use {
        reader ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = reader.read(buffer, offset, DEFAULT_BUFFER_SIZE)
            while (read != -1) {
                tmpFile.appendBytes(buffer)
                offset += read
                read = reader.read(buffer, offset, DEFAULT_BUFFER_SIZE)
            }
    }
    val destFile = File(FILE_DIR + fileName)
    tmpFile.renameTo(destFile)
}