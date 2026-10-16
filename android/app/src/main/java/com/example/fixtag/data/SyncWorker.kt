package com.example.fixtag.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.fixtag.FixTag
import retrofit2.HttpException
import java.io.IOException

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    ) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result {
        val repository = (applicationContext as FixTag).repository

        return try {
            // push pending data before pulling
            repository.pushPending()
            repository.pullMissing()
            Result.success()
        } catch (e: HttpException) {
            if (e.code() == 401) Result.failure() else Result.retry() // 401 means refresh failed
        } catch (e: IOException) {
            Result.retry()
        }
    }
}