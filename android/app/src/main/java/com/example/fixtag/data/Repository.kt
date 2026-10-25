package com.example.fixtag.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.example.fixtag.ATTACHMENTS_DIR
import com.example.fixtag.BuildConfig
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class Repository(
    private val db: Database,
    private val syncScheduler: SyncScheduler,
    private val apiService: ApiService,
    val filesDir: File,
    private val session: DataStore<Preferences>
) {

    private val currentUserId = stringPreferencesKey("current_user_id")

    // in-memory cache of the loggedInUser object
    var user: User? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    enum class DeleteResult { Deleted, NotPending, InUse }

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        user = null
    }

    // demo variant: choosing a profile is the whole login
    suspend fun loginDemo(userId: Uuid): User {
        seedDemoData()
        session.edit { it[currentUserId] = userId.toString() }
        return checkNotNull(db.getUserDao().get(userId)).also { user = it }
    }

    // fills an empty database once; later launches keep whatever changed during the demo
    private suspend fun seedDemoData() {
        if (db.getOrganizationDao().getAll().isNotEmpty()) return
        val now = Clock.System.now()
        db.withTransaction {  // parents before children, for the foreign keys
            db.getOrganizationDao().upsertAll(DemoSeed.organizations(now))
            DemoSeed.users.forEach { db.getUserDao().upsert(it) }
            db.getMachineDao().upsertAll(DemoSeed.machines(now))
            db.getIncidentDao().upsertAll(DemoSeed.incidents(now))
        }
    }

    suspend fun logout() {
        if (BuildConfig.OFFLINE_DEMO) session.edit { it.remove(currentUserId) }
        else db.getAuthDao().clear()    // otherwise the next launch restores the session again
        user = null
    }

    suspend fun restoreSession(): User? {
        user?.let { return it }
        val userId = if (BuildConfig.OFFLINE_DEMO) {
            session.data.first()[currentUserId]?.let(Uuid::parse)
        } else {
            db.getAuthDao().get()?.userId
        } ?: return null
        user = db.getUserDao().get(userId)
        return user
    }

    // non-demo variant: actually perform login
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
                authDao.upsert(auth)
                val user = User(
                    id = userId,
                    organizationId = orgId,
                    name = userData.name,
                    email = userData.email,
                    role = UserRole.fromCustomString(userData.role)
                )
                userDao.upsert(user)
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
                technicianDao.upsertAll(technicians)
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
                machineDao.upsertAll(machines)
                return Result.Success(machines)
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
                    incidentDao.upsert(dto.toEntity(incidentId))
                    incidentUpdateDao.upsertAll(dto.updates.map { it.toEntity(incidentId) })
                    attachmentDao.insertAll(dto.attachments.map { it.toEntity(incidentId)})
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

        val organizations = buildList {
            for (organization in pendingOrganizations) {
                val request = organization.toRequest()
                val response = apiService.createOrganization(organization.id.toString(), request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Log.i("Repository", "Successfully pushed organizations")
                    add(body.toEntity())
                }
            }
        }
        organizationDao.upsertAll(organizations)
        Log.i("Repository", "Successfully upserted ${organizations.size} organizations")
    }

    suspend fun pushPendingMachines() {
        val machineDao = db.getMachineDao()
        val pendingMachines = machineDao.getPending()

        if (pendingMachines.isEmpty()) {
            Log.i("Repository", "No pending machines to push")
            return
        }

        Log.i("Repository", "Pushing  ${pendingMachines.size} pending machines")
        val machines = buildList {
            for (machine in pendingMachines) {
                val request = machine.toRequest()
                val response = apiService.createMachine(machine.id.toString(), request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Log.i("Repository", "Successfully pushed machines")
                    add(body.toEntity())
                }
            }
        }
        machineDao.upsertAll(machines)
        Log.i("Repository", "Successfully upserted ${machines.size} machines")
    }

    suspend fun pushPendingIncidents() {
        val incidentDao = db.getIncidentDao()
        val pendingIncidents = incidentDao.getPending()

        if (pendingIncidents.isEmpty()) {
            Log.i("Repository", "No pending incidents to push")
            return
        }

        Log.i("Repository", "Pushing  ${pendingIncidents.size} pending incidents")
        val incidents = buildList {
            for (incident in pendingIncidents) {
                val request = incident.toRequest()
                val response = apiService.createIncident(incident.id.toString(), request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Log.i("Repository", "Successfully pushed incident")
                    add(body.toEntity(incident.id))
                }
            }
        }
        incidentDao.upsertAll(incidents)
        Log.i("Repository", "Successfully upserted ${incidents.size} incidents")
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

        val incidents = mutableListOf<Incident>()
        val updates = mutableListOf<IncidentUpdate>()

        pendingUpdates.forEach { update ->
            val request = update.toRequest()
            val response = apiService.addIncidentUpdate(update.incidentId.toString(), request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Log.i("Repository", "Successfully pushed incident update")
                val updatedIncident = body.toEntity(update.incidentId)
                incidents.add(updatedIncident)
                val syncedUpdate = body.updates.find { value -> value.id == update.id.toString() }
                if (syncedUpdate != null) {
                    updates.add(syncedUpdate.toEntity(update.incidentId))
                }
            }
        }
        // at once or nothing at all
        db.withTransaction {
            incidentDao.upsertAll(incidents)
            updateDao.upsertAll(updates)
        }
        Log.i(
            "Repository",
            "Successfully upserted ${updates.size} updates and their respective incidents"
        )
    }

    suspend fun pushPendingAttachments() {
        val attachmentDao = db.getAttachmentDao()
        val pendingAttachments = attachmentDao.getPending()

        if (pendingAttachments.isEmpty()) {
            Log.i("Repository", "No pending attachments to push")
            return
        }

        Log.i("Repository", "Pushing  ${pendingAttachments.size} pending attachments")

        val updateList = mutableListOf<Attachment>()
        val deleteList = mutableListOf<Attachment>()

        pendingAttachments.forEach { attachment ->
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
                when {
                    response.isSuccessful && body != null -> {
                        val syncedAttachment = body.toEntity(
                            attachment.incidentId,
                        )
                        syncedAttachment.path = attachment.path             // keep our local path
                        syncedAttachment.fetchStatus = SyncStatus.Synced
                        updateList.add(syncedAttachment)
                        Log.i("Repository", "Successfully pushed attachment")
                    }
                    response.code() >= 500 -> {
                        Log.i(
                            "Repository",
                            "Discarding temp attachments batch of ${updateList.size} updates and ${deleteList.size} deletes"
                        )
                        throw HttpException(response)
                    } // the worker retries with backoff
                    else -> attachment.syncStatus = SyncStatus.Failed       // 400, 413 too large, 415 wrong type
                }
            } else {
                // file does not exist so delete
                deleteList.add(attachment)
            }
        }
        db.withTransaction {
            attachmentDao.updateAll(updateList)
            attachmentDao.deleteAll(deleteList)
        }
        Log.i(
            "Repository",
            "Successfully updated ${updateList.size} and deleted ${deleteList.size} attachments"
        )
    }

    private val attachmentsDir = File(filesDir, ATTACHMENTS_DIR)

    suspend fun downloadAttachments() {
        val attachmentDao = db.getAttachmentDao()
        for (attachment in attachmentDao.getPendingFetch()) {
            val body = apiService.downloadAttachment(attachment.id.toString()).body() ?: continue  // retried next sync
            val file = File(attachmentsDir, "${attachment.id}.${attachment.kind.extension}")
            withContext(Dispatchers.IO) {
                attachmentsDir.mkdirs()
                val tmp = File.createTempFile("download", ".part", attachmentsDir)
                try {
                    body.byteStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
                    if (!tmp.renameTo(file)) throw IOException("couldn't rename $tmp")
                } finally {
                    tmp.delete()  // no-op after a successful rename
                }
            }
            attachment.path = file.absolutePath
            attachment.fetchStatus = SyncStatus.Synced
            attachmentDao.update(attachment)
        }
    }

    fun observeUser(): Flow<User?> {
        return db.getUserDao().observe()
    }

    fun observeOrganizations(): Flow<List<Organization>> {
        return db.getOrganizationDao().observeAll()
    }

    fun observeTechnicians(): Flow<List<Technician>> {
        return db.getTechnicianDao().observeAll()
    }

    fun observeIncidents(): Flow<List<IncidentAndMachine>> = db.getIncidentDao().observeAllWithMachine()

    fun observeIncidentsByMachine(machineId: Uuid): Flow<List<IncidentAndMachine>> =
        db.getIncidentDao().observeByMachineWithMachine(machineId)

    fun observeIncidentAndMachine(id: Uuid): Flow<IncidentAndMachine> {
        return db.getIncidentDao().observeWithMachine(id)
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

    fun observePendingCount() = db.getSyncStateDao().observePendingCount()

    fun observeMachinesWithStats() = db.getMachineDao().observeAllWithStats(since = Clock.System.now() - 30.days)

    suspend fun createOrganization(organization: Organization): Organization {
        val organizationDao = db.getOrganizationDao()
        organizationDao.upsert(organization)

        syncScheduler.syncNow()
        return organization
    }

    suspend fun createMachine(machine: Machine) {
        val machineDao = db.getMachineDao()
        machineDao.upsert(machine)

        syncScheduler.syncNow()
    }

    suspend fun createIncident(incident: Incident, attachments: List<Attachment>) {
        val incidentDao = db.getIncidentDao()
        val attachmentDao = db.getAttachmentDao()

        // write everything at once or nothing at all
        db.withTransaction {
            incidentDao.upsert(incident)
            attachmentDao.insertAll(attachments)
        }

        syncScheduler.syncNow()
    }

    suspend fun createAttachments(attachments: List<Attachment>) {
        val attachmentDao = db.getAttachmentDao()
        attachmentDao.insertAll(attachments)

        syncScheduler.syncNow()
    }

    suspend fun createIncidentUpdate(incidentUpdate: IncidentUpdate) {
        val incidentUpdateDao = db.getIncidentUpdateDao()
        incidentUpdateDao.upsert(incidentUpdate)

        syncScheduler.syncNow()
    }

    // a pending report only exists on this phone, so it goes together with its photos and messages
    suspend fun deletePendingIncident(id: Uuid): DeleteResult {
        var files = emptyList<String>()
        val result = db.withTransaction {
            val incident = db.getIncidentDao().get(id)
            if (incident == null || incident.syncStatus != SyncStatus.Pending) {
                return@withTransaction DeleteResult.NotPending
            }
            val attachments = db.getAttachmentDao().getByIncident(id)
            files = attachments.mapNotNull { it.path }
            db.getAttachmentDao().deleteAll(attachments)  // children first, for the foreign keys
            db.getIncidentUpdateDao().deleteByIncident(id)
            db.getIncidentDao().delete(incident)
            DeleteResult.Deleted
        }
        files.forEach { File(it).delete() }  // only once the rows are really gone
        return result
    }

    suspend fun deletePendingMachine(id: Uuid): DeleteResult = db.withTransaction {
        val machine = db.getMachineDao().get(id)
        when {
            machine == null || machine.syncStatus != SyncStatus.Pending -> DeleteResult.NotPending
            db.getIncidentDao().countByMachine(id) > 0 -> DeleteResult.InUse
            else -> { db.getMachineDao().delete(machine); DeleteResult.Deleted }
        }
    }

    suspend fun deletePendingOrganization(id: Uuid): DeleteResult = db.withTransaction {
        val organization = db.getOrganizationDao().get(id)
        when {
            organization == null || organization.syncStatus != SyncStatus.Pending -> DeleteResult.NotPending
            db.getMachineDao().countByOrganization(id) > 0 -> DeleteResult.InUse
            else -> { db.getOrganizationDao().delete(organization); DeleteResult.Deleted }
        }
    }

}