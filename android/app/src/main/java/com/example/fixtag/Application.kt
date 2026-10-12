package com.example.fixtag

import android.app.Application
import com.example.fixtag.data.Database
import com.example.fixtag.data.Repository
import com.example.fixtag.api.ApiClient

class FixTag : Application() {
    private val db by lazy { Database.getDatabase(this) }

    val repository by lazy {
        Repository(
            apiService = ApiClient.getApiService(this),
            authDao = db.getAuthDao(),
            userDao = db.getUserDao(),
            machineDao = db.getMachineDao(),
            incidentDao = db.getIncidentDao(),
            incidentUpdateDao = db.getIncidentUpdateDao(),
            attachmentDao = db.getAttachmentDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Your initialization code here
    }
}