package com.example.fixtag.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.fixtag.FixTag

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    ) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        val app = applicationContext as FixTag
        val repository = app.repository

        val unsyncedItems = repository.getPendingIncidentUploads()

        if (unsyncedItems.isEmpty()) return Result.success()

        var allSucceeded = true

        for (item in unsyncedItems) {

            // TODO
        }

        if (allSucceeded) {
            return ListenableWorker.Result.success()
        } else {
            return ListenableWorker.Result.retry()
        }
    }
}