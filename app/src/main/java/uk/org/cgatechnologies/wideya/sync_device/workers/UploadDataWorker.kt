package uk.org.cgatechnologies.wideya.sync_device.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.sync_device.SyncData

private const val TAG = "UploadDataWorker"

class UploadDataWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        Utils.insertLog(applicationContext, "🔄️  Sync started -- uploading")

        val attemptsLimit = 1000
        var attempts = 0

        val errorsLimit = 1
        var errors = 0

        do {
            val recordsCounter = SyncData.uploadData(applicationContext)
            Log.d(TAG, "Check Sync Upload Data Response: $recordsCounter")

            when {
                recordsCounter == SyncData.TOKEN_ERROR -> {
                    Utils.insertLog(applicationContext, "⚠️  Access token is invalid, try logging out then in again")
                    val data = Data.Builder()
                        .putString("token", "expired")
                        .putString("source", TAG)
                        .build()
                    return Result.failure(data)
                }
                recordsCounter == SyncData.SCHOOL_AUTH_ERROR -> {
                    Utils.insertLog(applicationContext, "⚠️  You have no permission to this school")
                    val data = Data.Builder()
                        .putString("school", "school_unauthorized")
                        .putString("source", TAG)
                        .build()
                    return Result.failure(data)
                }
                recordsCounter == SyncData.NETWORK_ERROR -> {
                    errors += 1
                    Utils.insertLog(applicationContext, "⚠️  An error occurred, check network")
                    Result.failure()
                }
                recordsCounter == 0 -> {
//                    Utils.insertLog(applicationContext, "No records to upload")
                    return Result.success()
                }
                recordsCounter > 0 -> {
                    Utils.insertLog(applicationContext, "⬆️  $recordsCounter records sent to server")
                }
                else -> {
                    Result.failure()
                }
            }

            attempts += 1

        } while (
            (recordsCounter == 0).not()
                .and(attempts < attemptsLimit)
                .and(errors < errorsLimit)
        )

        return if (errors == errorsLimit) {
            Result.failure()
        } else if (attempts == attemptsLimit){
            Utils.insertLog(applicationContext, "⚠️  Uploading unfinished")
            Result.success()
        } else {
            Result.success()
        }
    }
}