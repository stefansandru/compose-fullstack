package com.example.composetutorial.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.composetutorial.ComposeTutorialApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.composetutorial.util.NotificationHelper

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as ComposeTutorialApplication
        val repository = app.container.repository
        val notificationHelper = NotificationHelper(applicationContext)

        return@withContext try {
            Log.d("SyncWorker", "Starting background sync...")
            repository.sync() // Push local changes
            repository.refreshItems() // Pull remote changes
            Log.d("SyncWorker", "Background sync completed successfully")
            
            notificationHelper.showNotification(
                "Data Synced",
                "Application data has been successfully updated from the server."
            )
            
            Result.success()
        } catch (e: retrofit2.HttpException) {
            Log.e("SyncWorker", "HTTP Error during sync: ${e.code()}", e)
            if (e.code() == 403 || e.code() == 401) {
                notificationHelper.showNotification(
                    "Sync Failed",
                    "Session expired. Please log in again."
                )
                Result.failure()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Background sync failed", e)
            Result.retry()
        }
    }
}
