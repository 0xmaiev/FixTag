package com.example.fixtag.data

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Name for the one time sync work
const val SYNC: String = "sync"

// Name for the periodic work
const val SYNC_PERIODIC: String = "sync-periodic"

// Interval between periodic syncs
const val SYNC_PERIOD: Long = 15

// TODO: Add datastore for preferences to store connection type configs

class SyncScheduler(private val workManager: WorkManager) {
    private val online = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            //.setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(online)
            .build()
        workManager.enqueueUniqueWork(
            SYNC,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_PERIOD,
            TimeUnit.MINUTES
            )
            .setConstraints(online)
            .build()
        workManager.enqueueUniquePeriodicWork(
            SYNC_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}