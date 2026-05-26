package uk.org.cgatechnologies.wideya.sync_device.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.sync_device.SyncData

private const val TAG = "DownloadDataWorker"

class DownloadDataWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        Utils.insertLog(applicationContext, "🔄  Sync started -- downloading")

        val attemptsLimit = 1000
        var attempts = 0

        val errorsLimit = 1
        var errors = 0

        do {
            val recordsCounter = SyncData.downloadData(applicationContext)

            Log.d(TAG, "Check Sync Download Data Response: $recordsCounter")

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
                recordsCounter == -1 -> {
                    errors += 1
                    Utils.insertLog(applicationContext, "⚠️  An error occurred, check network")
                }
                recordsCounter == 0 -> {
                    Utils.insertLog(applicationContext, "✅✅  Database up to date")
                }
                recordsCounter > 0 -> {
                    // commented out because was cluttering the logs and duplicating previous log info
//                    Utils.insertLog(applicationContext, "$recordsCounter records inserted")
                }
            }

            attempts += 1

        } while (
                (recordsCounter == 0).not()
                .and(attempts < attemptsLimit)
                .and(errors < errorsLimit)
        )

        return if (attempts == attemptsLimit || errors == errorsLimit) {
            Result.failure()
        } else {
            Result.success()
        }
    }
}