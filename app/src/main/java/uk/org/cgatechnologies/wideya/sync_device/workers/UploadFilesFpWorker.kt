package uk.org.cgatechnologies.wideya.sync_device.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.sync_device.SyncFiles

private const val TAG = "UploadFilesFpWorker"

class UploadFilesFpWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val filesCounter = SyncFiles.uploadFingerprints(applicationContext)

        return when {
            filesCounter == -3 -> {
                Utils.insertLog(applicationContext, "⚠️  S3 error, try again later or contact admin")
                Result.failure()
            }
            filesCounter == -1 -> {
                Utils.insertLog(applicationContext, "⚠️  An error occurred, check network")
                Result.failure()
            }
            filesCounter == 0 -> {
//                Utils.insertLog(applicationContext, "No fp files to upload")
                Result.success()
            }
            filesCounter > 0 -> {
                Utils.insertLog(applicationContext, "⬆️  $filesCounter fingerprint files sent to server")
                Result.success()
            }
            else -> {
                Result.failure()
            }
        }
    }


}