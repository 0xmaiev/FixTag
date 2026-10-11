package com.example.fixtag.data

import android.util.Log
import com.example.fixtag.api.ApiService
import com.example.fixtag.api.LoginRequest
import com.example.fixtag.data.model.Auth
import com.example.fixtag.data.model.AuthDao
import com.example.fixtag.data.model.Incident
import com.example.fixtag.data.model.IncidentDao
import com.example.fixtag.data.model.User
import com.example.fixtag.data.model.UserDao
import kotlin.uuid.Uuid

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class Repository(
    private val apiService: ApiService,
    private val authDao: AuthDao,
    private val userDao: UserDao,
    private val incidentDao: IncidentDao
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
                    role = userData.role
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

    suspend fun getPendingIncidentUploads(): List<Incident> {
        val pending = incidentDao.getAll()

        return pending
    }
}