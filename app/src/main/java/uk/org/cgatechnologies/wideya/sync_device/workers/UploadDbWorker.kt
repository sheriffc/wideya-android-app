package uk.org.cgatechnologies.wideya.sync_device.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import uk.org.cgatechnologies.wideya.sync_device.SyncFiles

private const val TAG = "UploadDbWorker"

class UploadDbWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        return try {
            if (SyncFiles.uploadDatabaseBackup(applicationContext)) {
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
            Result.failure()
        }
    }
}