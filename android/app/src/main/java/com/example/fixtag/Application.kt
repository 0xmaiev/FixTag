package com.example.fixtag

import android.app.Application
import androidx.work.WorkManager
import com.example.fixtag.data.Database
import com.example.fixtag.data.Repository
import com.example.fixtag.api.ApiClient
import com.example.fixtag.data.SyncScheduler

class FixTag : Application() {
    private val db by lazy { Database.getDatabase(this) }

    private val workManager by lazy {
        WorkManager.getInstance(this)
    }

    val syncScheduler by lazy {
        SyncScheduler(workManager)
    }

    val repository by lazy {
        Repository(
            db = db,
            syncScheduler = syncScheduler,
            apiService = ApiClient.getApiService(this),
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Your initialization code here
    }
}