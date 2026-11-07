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
import com.example.fixtag.api.PasswordChangeRequestDto
import com.example.fixtag.api.model.TokenResponseDto
import com.example.fixtag.data.model.Attachment
import com.example.fixtag.data.model.Auth
import com.example.fixtag.data.model.ClientManufacturerLink
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentAndMachine
import com.example.fixtag.data.model.IncidentUpdate
import com.example.fixtag.data.model.Machine
import com.example.fixtag.data.model.MachineWithStats
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.OrganizationKind
import com.example.fixtag.data.model.SyncState
import com.example.fixtag.data.model.SyncStatus
import com.example.fixtag.data.model.TechnicianWithLoad
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserRole
import com.example.fixtag.data.model.isLocalOnly
import com.example.fixtag.data.model.scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

    // the signed-in user as a flow too: when Android restores the app after killing it, the screens
    // come back before restoreSession() finishes, and their lists fill in once it does
    private val currentUser = MutableStateFlow<User?>(null)
    val signedIn: StateFlow<User?> = currentUser.asStateFlow()

    // in-memory cache of the user and his organization
    var user: User?
        get() = currentUser.value
        private set(value) { currentUser.value = value }

    // a list for the signed-in user, empty while there's none
    private fun <T> whenSignedIn(query: (User) -> Flow<List<T>>): Flow<List<T>> =
        currentUser.flatMapLatest { user -> if (user == null) flowOf(emptyList()) else query(user) }

    enum class DeleteResult { Deleted, NotPending, InUse }

    // what registering a client did, as POST /clients answers it
    sealed interface Registration {
        data class Registered(val client: Organization) : Registration  // 201, with its first gestor
        data class Linked(val client: Organization) : Registration      // 200, the NIF was known
        data object NifIsAManufacturer : Registration                   // 409
        data object EmailTaken : Registration                           // 409
    }

    // what changing the password did, as POST /me/password answers it
    sealed interface PasswordChange {
        data class Changed(val user: User) : PasswordChange  // 200, with new tokens
        data object WrongPassword : PasswordChange           // 400, the current password didn't match
        data object Failed : PasswordChange                  // offline, or any other answer
    }

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
            db.getClientManufacturerLinkDao().upsertAll(DemoSeed.links(now))
            db.getUserDao().upsertAll(DemoSeed.users)
            db.getMachineDao().upsertAll(DemoSeed.machines(now))
            db.getIncidentDao().upsertAll(DemoSeed.incidents(now))
            db.getIncidentUpdateDao().upsertAll(DemoSeed.incidentUpdates(now))
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
                return Result.Success(saveSession(response.body()!!))
            }

            return Result.Error(Exception("Invalid response body."))

        } catch (e: Exception) {
            Log.d("Repository", e.toString())
            return Result.Error(e)
        }
    }

    // /login and /me/password answer alike: both tokens, and the user they belong to
    private suspend fun saveSession(body: TokenResponseDto): User {
        val userData = body.userDto
        Log.d("Repository", "Got $userData")
        val user = User(
            id = Uuid.parse(userData.id),
            organizationId = Uuid.parse(userData.organizationId),
            organizationKind = userData.organizationKind,
            name = userData.name,
            email = userData.email,
            role = userData.role,
            mustChangePassword = userData.mustChangePassword,
            syncStatus = SyncStatus.Synced
        )
        db.getAuthDao().upsert(Auth(
            userId = user.id,
            refreshToken = body.refreshToken,
            accessToken = body.accessToken
        ))
        db.getUserDao().upsert(user)
        this.user = user
        return user
    }

    // the server swaps the temporary password and answers like /login. The old access token still
    // says must_change_password and the server deleted the old refresh tokens, so the new pair
    // replaces them, and there's no need to sign in again
    suspend fun changePassword(currentPassword: String, newPassword: String): PasswordChange {
        try {
            val response = apiService.changePassword(PasswordChangeRequestDto(currentPassword, newPassword))
            Log.d("Repository", "Got ${response.code()} from /me/password")
            val body = response.body()
            return when {
                response.isSuccessful && body != null -> PasswordChange.Changed(saveSession(body))
                // the form already checks the length and that it changed, which leaves the current password
                response.code() == 400 -> PasswordChange.WrongPassword
                else -> PasswordChange.Failed
            }
        } catch (e: Exception) {
            Log.d("Repository", e.toString())
            return PasswordChange.Failed
        }
    }

    suspend fun refreshMembers(): Result<List<User>> {
        try {
            Log.d("Repository", "Preparing technicians request")
            val response = apiService.getMembers()

            Log.d("Repository", "Got ${response.code()} from /technicians")
            if (response.isSuccessful && response.body() != null) {
                val userDao = db.getUserDao()
                val body = response.body()!!
                val members = body.map { it.toEntity() }
                userDao.upsertAll(members)
                return Result.Success(members)
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
                // the server answers the caller's own organization plus every one linked to it, so
                // each organization of the other kind is a link (the clients screen reads them)
                val me = checkNotNull(user)
                val links = organizations.filter { it.kind != me.organizationKind }.map { other ->
                    val (client, manufacturer) =
                        if (me.organizationKind == OrganizationKind.Cliente) me.organizationId to other.id
                        else other.id to me.organizationId
                    ClientManufacturerLink(client, manufacturer, Clock.System.now(), SyncStatus.Synced)
                }
                db.withTransaction {  // the links point at the organizations
                    organizationDao.upsertAll(organizations)
                    db.getClientManufacturerLinkDao().upsertAll(links)
                }
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

    //  organizations -> technicians -> incidents -> attachments / updates (server FK order)
    suspend fun pullMissing() {
        refreshOrganizations()
        refreshMembers()
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

    // the clients the signed-in manufacturer serves; only its support staff open these screens
    fun observeOrganizations(): Flow<List<Organization>> =
        whenSignedIn { db.getOrganizationDao().observeClients(it.organizationId) }

    // support staff hand incidents to their own organization's technicians, as the server checks
    fun observeTechnicians(): Flow<List<TechnicianWithLoad>> =
        whenSignedIn { db.getUserDao().observeTechnicians(it.organizationId) }

    fun observeMembers(): Flow<List<User>> =
        whenSignedIn { db.getUserDao().observeMembers(it.organizationId) }

    // the demo phone holds every organization, so each list only shows what the server would send
    // the signed-in profile (User.scope)
    fun observeIncidents(): Flow<List<IncidentAndMachine>> = observeVisibleIncidents(machine = null)

    fun observeIncidentsByMachine(machineId: Uuid): Flow<List<IncidentAndMachine>> =
        observeVisibleIncidents(machineId)

    private fun observeVisibleIncidents(machine: Uuid?): Flow<List<IncidentAndMachine>> =
        whenSignedIn { user ->
            val scope = user.scope
            db.getIncidentDao().observeVisibleWithMachine(
                machine, scope.client, scope.manufacturer, scope.technician?.toString())
        }

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

    fun observeMachines(): Flow<List<Machine>> =
        whenSignedIn { db.getMachineDao().observeVisible(it.scope.client, it.scope.manufacturer) }

    fun observePendingCount() = db.getSyncStateDao().observePendingCount()

    fun observeMachinesWithStats(): Flow<List<MachineWithStats>> =
        whenSignedIn {
            db.getMachineDao().observeVisibleWithStats(
                it.scope.client, it.scope.manufacturer, since = Clock.System.now() - 30.days)
        }

    suspend fun getIncident(id: Uuid): Incident? = db.getIncidentDao().get(id)

    suspend fun isSerialNumberTaken(serialNumber: String, exceptId: Uuid) =
        db.getMachineDao().isSerialNumberTaken(serialNumber, exceptId)

    suspend fun isEmailTaken(email: String) = db.getUserDao().isEmailTaken(email)

    // what POST /clients does: a NIF already on this phone only gets linked to the signed-in
    // manufacturer, otherwise the client is new and so is its first gestor, who then creates the
    // rest of its accounts
    suspend fun registerClient(client: Organization, managerName: String, managerEmail: String): Registration {
        val manufacturerId = checkNotNull(user).organizationId
        val result = db.withTransaction {
            val existing = db.getOrganizationDao().findByNif(client.nif)
            if (existing != null && existing.kind != OrganizationKind.Cliente) {
                return@withTransaction Registration.NifIsAManufacturer
            }
            if (existing == null) {
                // the gestor is only created with a new client, so only then can the email clash
                if (db.getUserDao().isEmailTaken(managerEmail)) {
                    return@withTransaction Registration.EmailTaken
                }
                db.getOrganizationDao().upsert(client)
                db.getUserDao().upsert(User.create(
                    Uuid.random(), client.id, client.kind, managerName, managerEmail, UserRole.Gestor))
            }
            val registered = existing ?: client
            db.getClientManufacturerLinkDao().upsert(
                ClientManufacturerLink(registered.id, manufacturerId, Clock.System.now()))
            if (existing == null) Registration.Registered(registered) else Registration.Linked(registered)
        }
        syncScheduler.syncNow()
        return result
    }

    suspend fun createUser(user: User): User {
        val userDao = db.getUserDao()
        userDao.upsert(user)

        syncScheduler.syncNow()
        return user
    }

    suspend fun createMachine(machine: Machine) {
        val machineDao = db.getMachineDao()
        machineDao.upsert(machine)

        syncScheduler.syncNow()
    }

    // an upsert, so the author's edit of an incident is saved the same way
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

    // Room has no trigger like the server's incident_updates_after_insert, so the incident takes the
    // new status and technician here, in the same transaction. Its sync status stays as it was:
    // the update is what gets sent, and the server applies it there the same way
    suspend fun createIncidentUpdate(incidentUpdate: IncidentUpdate) {
        db.withTransaction {
            db.getIncidentUpdateDao().upsert(incidentUpdate)
            val incident = checkNotNull(db.getIncidentDao().get(incidentUpdate.incidentId))
            db.getIncidentDao().upsert(incident.copy(
                status = incidentUpdate.newStatus ?: incident.status,
                assignedTo = incidentUpdate.assignedTo?.let(Uuid::parse) ?: incident.assignedTo,
                assignedToName = incidentUpdate.assignedToName ?: incident.assignedToName,
            ))
        }
        syncScheduler.syncNow()
    }

    // a pending report only exists on this phone, so it goes together with its photos and messages
    suspend fun deletePendingIncident(id: Uuid): DeleteResult {
        var files = emptyList<String>()
        val result = db.withTransaction {
            val incident = db.getIncidentDao().get(id)
            if (incident == null || !incident.isLocalOnly) {
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