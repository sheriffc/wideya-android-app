package uk.org.cgatechnologies.wideya.sync_device.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.utils.Utils
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class WorkerHelper {
    companion object {

        fun pruneWorkersData(context: Context) {
            WorkManager.getInstance(context).pruneWork()
        }

        fun cancelBackgroundSync(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(Constants.WORKER_SYNC_GROUP_TAG)
        }

        fun initBackgroundSync(context: Context) {
            cancelBackgroundSync(context)

            if (Utils.getEncSharedPrefs(context).getBoolean(Constants.PREF_BACKGROUND_SYNC_BOOL, true)) {
                enqueueBackgroundSync(context)
            }
        }

        fun initBackgroundSyncOnStart(context: Context) {
            if (Utils.getEncSharedPrefs(context).getBoolean(Constants.PREF_BACKGROUND_SYNC_BOOL, true)) {
                enqueueBackgroundSync(context)
            }
        }

        private fun enqueueBackgroundSync(context: Context) {
            val constraints = commonWorkConstraints()

            val downloadWorker = PeriodicWorkRequest.Builder(DownloadDataWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_PERIODIC_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()

            val uploadWorker = PeriodicWorkRequest.Builder(UploadDataWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_PERIODIC_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()

            val uploadFilesFpWorker = PeriodicWorkRequest.Builder(UploadFilesFpWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .addTag(Constants.WORKER_PERIODIC_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()

            val uploadFilesTeacherProfilePhotosWorker =
                PeriodicWorkRequest.Builder(UploadFilesTeacherProfilePhotosWorker::class.java, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setInitialDelay(1, TimeUnit.MINUTES)
                    .addTag(Constants.WORKER_PERIODIC_SYNC_TAG)
                    .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                    .build()

            val uploadFilesTeacherAttPhotosWorker =
                PeriodicWorkRequest.Builder(UploadFilesTeacherAttPhotosWorker::class.java, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setInitialDelay(1, TimeUnit.MINUTES)
                    .addTag(Constants.WORKER_PERIODIC_SYNC_TAG)
                    .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                    .build()

            WorkManager.getInstance(context).apply {
                enqueueUniquePeriodicWork(
                    Constants.WORKER_UPLOAD_SYNC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    uploadWorker
                )
                enqueueUniquePeriodicWork(
                    Constants.WORKER_UPLOAD_FP_SYNC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    uploadFilesFpWorker
                )
                enqueueUniquePeriodicWork(
                    Constants.WORKER_UPLOAD_TEACHER_PROFILE_PHOTOS_SYNC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    uploadFilesTeacherProfilePhotosWorker
                )
                enqueueUniquePeriodicWork(
                    Constants.WORKER_UPLOAD_TEACHER_ATT_PHOTOS_SYNC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    uploadFilesTeacherAttPhotosWorker
                )
                enqueueUniquePeriodicWork(
                    Constants.WORKER_DOWNLOAD_SYNC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    downloadWorker
                )
            }
        }

        private fun commonWorkConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        }

        fun initUniqueUploadWorkRequest(context: Context) {
            val uploadWorkRequest = OneTimeWorkRequest.Builder(UploadDataWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()
            val uploadFilesFpWorker = OneTimeWorkRequest.Builder(UploadFilesFpWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()
            val uploadFilesTeacherProfilePhotosWorker =
                OneTimeWorkRequest.Builder(UploadFilesTeacherProfilePhotosWorker::class.java)
                    .setConstraints(commonWorkConstraints())
                    .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                    .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                    .build()
            val uploadFilesTeacherAttPhotosWorker =
                OneTimeWorkRequest.Builder(UploadFilesTeacherAttPhotosWorker::class.java)
                    .setConstraints(commonWorkConstraints())
                    .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                    .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                    .build()

            WorkManager.getInstance(context)
                .beginUniqueWork(Constants.WORKER_UPLOAD_SYNC, ExistingWorkPolicy.KEEP, uploadWorkRequest)
                .then(uploadFilesFpWorker)
                .then(uploadFilesTeacherProfilePhotosWorker)
                .then(uploadFilesTeacherAttPhotosWorker)
                .enqueue()
        }


        fun initCheckinThenUniqueUploadWorkRequest(context: Context) {
            val checkInWorker = OneTimeWorkRequest.Builder(CheckInWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_CHECK_IN_ONETIME_TAG)
                .build()
            val uploadWorkRequest = OneTimeWorkRequest.Builder(UploadDataWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()
            val uploadFilesFpWorker = OneTimeWorkRequest.Builder(UploadFilesFpWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()
            val uploadFilesTeacherProfilePhotosWorker =
                OneTimeWorkRequest.Builder(UploadFilesTeacherProfilePhotosWorker::class.java)
                    .setConstraints(commonWorkConstraints())
                    .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                    .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                    .build()
            val uploadFilesTeacherAttPhotosWorker =
                OneTimeWorkRequest.Builder(UploadFilesTeacherAttPhotosWorker::class.java)
                    .setConstraints(commonWorkConstraints())
                    .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                    .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                    .build()

            WorkManager.getInstance(context)
                .beginUniqueWork(Constants.WORKER_UPLOAD_SYNC, ExistingWorkPolicy.KEEP, checkInWorker)
                .then(uploadWorkRequest)
                .then(uploadFilesFpWorker)
                .then(uploadFilesTeacherProfilePhotosWorker)
                .then(uploadFilesTeacherAttPhotosWorker)
                .enqueue()
        }

        fun initUniqueDownloadWorkRequest(context: Context) {
            val downloadWorkRequest = OneTimeWorkRequest.Builder(DownloadDataWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_DOWNLOAD_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(Constants.WORKER_DOWNLOAD_SYNC, ExistingWorkPolicy.KEEP, downloadWorkRequest)
        }

        fun initUniqueSyncWorkRequest(context: Context) {
            val uploadWorkRequest = OneTimeWorkRequest.Builder(UploadDataWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()
            val uploadFilesFpWorker = OneTimeWorkRequest.Builder(UploadFilesFpWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()
            val uploadFilesTeacherProfilePhotosWorker =
                OneTimeWorkRequest.Builder(UploadFilesTeacherProfilePhotosWorker::class.java)
                    .setConstraints(commonWorkConstraints())
                    .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                    .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                    .build()
            val uploadFilesTeacherAttPhotosWorker =
                OneTimeWorkRequest.Builder(UploadFilesTeacherAttPhotosWorker::class.java)
                    .setConstraints(commonWorkConstraints())
                    .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                    .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                    .build()

            val downloadWorkRequest = OneTimeWorkRequest.Builder(DownloadDataWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_DOWNLOAD_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()

            WorkManager.getInstance(context)
                .beginUniqueWork(Constants.WORKER_SYNC, ExistingWorkPolicy.KEEP, uploadWorkRequest)
                .then(uploadFilesFpWorker)
                .then(uploadFilesTeacherProfilePhotosWorker)
                .then(uploadFilesTeacherAttPhotosWorker)
                .then(downloadWorkRequest)
                .enqueue()
        }


        fun initCheckinThenUniqueSyncWorkRequest(context: Context) {
            val checkInWorker = OneTimeWorkRequest.Builder(CheckInWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_CHECK_IN_ONETIME_TAG)
                .build()
            val uploadWorkRequest = OneTimeWorkRequest.Builder(UploadDataWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()
            val uploadFilesFpWorker = OneTimeWorkRequest.Builder(UploadFilesFpWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()
            val uploadFilesTeacherProfilePhotosWorker =
                OneTimeWorkRequest.Builder(UploadFilesTeacherProfilePhotosWorker::class.java)
                    .setConstraints(commonWorkConstraints())
                    .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                    .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                    .build()
            val uploadFilesTeacherAttPhotosWorker =
                OneTimeWorkRequest.Builder(UploadFilesTeacherAttPhotosWorker::class.java)
                    .setConstraints(commonWorkConstraints())
                    .addTag(Constants.WORKER_UPLOAD_SYNC_TAG)
                    .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                    .build()
            val downloadWorkRequest = OneTimeWorkRequest.Builder(DownloadDataWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_DOWNLOAD_SYNC_TAG)
                .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                .build()

            WorkManager.getInstance(context)
                .beginUniqueWork(Constants.WORKER_SYNC, ExistingWorkPolicy.KEEP, checkInWorker)
                .then(uploadWorkRequest)
                .then(uploadFilesFpWorker)
                .then(uploadFilesTeacherProfilePhotosWorker)
                .then(uploadFilesTeacherAttPhotosWorker)
                .then(downloadWorkRequest)
                .enqueue()
        }

        fun enqueuePeriodicCheckIn(context: Context) {
            val constraints = commonWorkConstraints()

            val checkInWorker = PeriodicWorkRequest.Builder(CheckInWorker::class.java, 2, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_CHECK_IN_TAG)
                .build()

            WorkManager.getInstance(context).apply {
                enqueueUniquePeriodicWork(
                    Constants.WORKER_CHECK_IN,
                    ExistingPeriodicWorkPolicy.KEEP,
                    checkInWorker
                )
            }
        }

        fun initUniqueOneTimeCheckIn(context: Context) {
            val checkInWorker = OneTimeWorkRequest.Builder(CheckInWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_CHECK_IN_ONETIME_TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(Constants.WORKER_CHECK_IN_ONETIME, ExistingWorkPolicy.KEEP, checkInWorker)
        }

        fun initInitialDownloadWorker(context: Context) {
            val initialDownloadWorkRequest =
                OneTimeWorkRequestBuilder<DownloadDataInitialiseWorker>()
                    .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                    .addTag(Constants.WORKER_INITIAL_DOWNLOAD_SYNC_TAG)
                    .addTag(Constants.WORKER_SYNC_GROUP_TAG)
                    .build()

            WorkManager
                .getInstance(context)
                .beginUniqueWork(
                    Constants.WORKER_INITIAL_DOWNLOAD_SYNC,
                    ExistingWorkPolicy.KEEP,
                    initialDownloadWorkRequest
                )
                .enqueue()
        }

        fun initUniqueOneTimeUploadDb(context: Context) {

            val uploadDbWorker = OneTimeWorkRequest.Builder(UploadDbWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.WORKER_UPLOAD_DB_ONETIME_TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(Constants.WORKER_UPLOAD_DB_ONETIME, ExistingWorkPolicy.KEEP, uploadDbWorker)
        }

        fun initUniqueOneTimeCheckInAndUploadDb(context: Context) {
            val checkInWorker = OneTimeWorkRequest.Builder(CheckInWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.TOKEN_RELATED_WORKER_TAG)
                .addTag(Constants.WORKER_CHECK_IN_ONETIME_TAG)
                .build()

            val uploadDbWorker = OneTimeWorkRequest.Builder(UploadDbWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.WORKER_UPLOAD_DB_ONETIME_TAG)
                .build()

            WorkManager.getInstance(context)
                .beginUniqueWork(Constants.WORKER_UPLOAD_DB_ONETIME, ExistingWorkPolicy.KEEP, checkInWorker)
                .then(uploadDbWorker)
                .enqueue()
        }

        fun initUniqueOneTimeCheckAppUpdate(context: Context) {
            val checkAppUpdateWorker = OneTimeWorkRequest.Builder(CheckAppUpdateWorker::class.java)
                .setConstraints(commonWorkConstraints())
                .addTag(Constants.WORKER_CHECK_APP_UPDATE_ONETIME_TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    Constants.WORKER_CHECK_APP_UPDATE_ONETIME,
                    ExistingWorkPolicy.KEEP,
                    checkAppUpdateWorker
                )
        }

        fun scheduleNoSchoolWorker(
            context: Context,
            schoolUuid: String,
            dateStr: String,
            reasonOid: String,
            reasonOther: String?
        ) {
            val targetMidnightMs = LocalDate.parse(dateStr)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val delayMs = (targetMidnightMs - System.currentTimeMillis()).coerceAtLeast(0L)

            val inputData = Data.Builder()
                .putString(NoSchoolWorker.KEY_SCHOOL_UUID, schoolUuid)
                .putString(NoSchoolWorker.KEY_DATE, dateStr)
                .putString(NoSchoolWorker.KEY_REASON_OID, reasonOid)
                .apply { reasonOther?.let { putString(NoSchoolWorker.KEY_REASON_OTHER, it) } }
                .build()

            val workRequest = OneTimeWorkRequest.Builder(NoSchoolWorker::class.java)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${Constants.WORKER_NO_SCHOOL_PREFIX}${schoolUuid}_$dateStr",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}