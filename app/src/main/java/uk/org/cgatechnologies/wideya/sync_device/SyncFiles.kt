package uk.org.cgatechnologies.wideya.sync_device

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3Client
import com.haroldadmin.cnradapter.NetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.org.cgatechnologies.wideya.BuildConfig
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.Secrets
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.network.RetrofitBuilder
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.utils.Utils.byteArrayToBase64
import uk.org.cgatechnologies.wideya.common.utils.Utils.xorEncryptExt
import uk.org.cgatechnologies.wideya.sync_device.inferfaces.SyncApi
import uk.org.cgatechnologies.wideya.sync_device.models.TokenRequest
import uk.org.cgatechnologies.wideya.sync_device.models.TokenResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val TAG = "SyncFiles"

class SyncFiles {

    companion object {

        private fun s3Client(context: Context): AmazonS3Client {
            val accessKey = Utils.getEncSharedPrefs(context).getString(Constants.PREF_S3_KEY, "")
            val secretKey = Utils.getEncSharedPrefs(context).getString(Constants.PREF_S3_SECRET, "")
            val regionString = Utils.getEncSharedPrefs(context).getString(Constants.PREF_S3_REGION, "")
            val region = Region.getRegion(regionString)
            val credentials = BasicAWSCredentials(accessKey, secretKey)
            val provider = StaticCredentialsProvider(credentials)
            return AmazonS3Client(provider, region)
        }

        private fun s3CredentialsAvailable(context: Context): Boolean {
            val key = Utils.getEncSharedPrefs(context).getString(Constants.PREF_S3_KEY, "")
            val secret = Utils.getEncSharedPrefs(context).getString(Constants.PREF_S3_SECRET, "")
            val bucket = Utils.getEncSharedPrefs(context).getString(Constants.PREF_S3_BUCKET, "")
            return !key.isNullOrEmpty() && !secret.isNullOrEmpty() && !bucket.isNullOrEmpty()
        }

        private fun uploadFileToServer(context: Context, file: File, fileType: String): Boolean {
            val accessToken = Utils.getEncSharedPrefs(context).getString(Constants.PREF_ACCESS_TOKEN, "") ?: ""
            val client = OkHttpClient()
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("access_token", accessToken)
                .addFormDataPart("request_type", fileType)
                .addFormDataPart("file", file.name, file.asRequestBody("application/octet-stream".toMediaType()))
                .build()
            val request = okhttp3.Request.Builder()
                .url("${BuildConfig.SERVER_URL}mobile/sync/upload-file")
                .post(body)
                .build()
            return try {
                client.newCall(request).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                Log.e(TAG, "Server file upload failed: ${e.message}")
                false
            }
        }

        private fun uploadStringToServer(context: Context, filename: String, content: String, fileType: String): Boolean {
            val accessToken = Utils.getEncSharedPrefs(context).getString(Constants.PREF_ACCESS_TOKEN, "") ?: ""
            val client = OkHttpClient()
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("access_token", accessToken)
                .addFormDataPart("request_type", fileType)
                .addFormDataPart("filename", filename)
                .addFormDataPart("file_content", content)
                .build()
            val request = okhttp3.Request.Builder()
                .url("${BuildConfig.SERVER_URL}mobile/sync/upload-file")
                .post(body)
                .build()
            return try {
                client.newCall(request).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                Log.e(TAG, "Server file upload failed: ${e.message}")
                false
            }
        }

        suspend fun uploadDatabaseBackup(context: Context): Boolean {
            val dataDir = Environment.getDataDirectory()
            val packageName: String = context.packageName

            val dbFilename = Constants.DB_FILENAME

            val dbExtensions: MutableList<String> = ArrayList()

            val dbDataPaths: MutableList<String> = ArrayList()

            dbExtensions.add("")
            dbExtensions.add("-shm")
            dbExtensions.add("-wal")

            val currentDBPath = "//data//$packageName//databases//$dbFilename"

            dbExtensions.forEach {
                val pathWithExtension = "${currentDBPath}$it"
                if (File(dataDir, pathWithExtension).exists()) {
                    dbDataPaths.add(pathWithExtension)
                }
            }

            if (dbDataPaths.isEmpty()) return false

            //create the zip file
            val dateTimeFn = Utils.getISODateTimeUTCFileFriendly()

            //create tmp dir if it doesn't exist
            val tmpDir = File("${context.filesDir.absolutePath}${File.separator}${Constants.PATH_TMP}")
            if (tmpDir.exists().not()) tmpDir.mkdirs()

            val zipFile = File(tmpDir, "${dateTimeFn}.zip")

            //delete the zip if it exists
            if (zipFile.exists()) zipFile.delete()

            withContext(Dispatchers.IO) {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { out ->
                    dbDataPaths.forEach { filePath ->
                        val file = File(dataDir, filePath)
                        FileInputStream(file).use { fi ->
                            BufferedInputStream(fi).use { origin ->
                                val entry = ZipEntry(filePath.substring(filePath.lastIndexOf(File.separator)))
                                out.putNextEntry(entry)
                                origin.copyTo(out, 1024)
                            }
                        }
                    }
                }
            }

            val userId = Utils.getUserId(context)

            val s3Bucket = Utils.getEncSharedPrefs(context).getString(Constants.PREF_S3_BUCKET, "")

            val buildVariant = BuildConfig.BUILD_TYPE

            val remoteBucket = "$s3Bucket/mobile/$buildVariant/backups/$userId/${Utils.getISODateUTC()}"
            val remoteKey = "wideya_${Utils.getISODateTimeUTCFileFriendly()}.zip"

            if (zipFile.exists().not()) return false
            if (zipFile.canRead().not()) return false

            return try {
                s3Client(context).putObject(remoteBucket, remoteKey, zipFile)
                zipFile.delete()
                uploadDbReceipt(context, remoteBucket, remoteKey)
                true
            } catch (e: AmazonServiceException) {
                Log.e(TAG, e.message.toString())
                zipFile.delete()
                false
            }
        }

        private suspend fun uploadDbReceipt(context: Context, path: String, filename: String){

            val retrofit = RetrofitBuilder.getRetrofit(BuildConfig.SERVER_URL)
            val service = retrofit.create(SyncApi::class.java)

            val params = mutableMapOf<String, String>()

            params.apply {
                put(
                    "username",
                    Utils.getEncSharedPrefs(context).getString(Constants.PREF_USERNAME, "").toString()
                )
                put(
                    "install_id",
                    Utils.getEncSharedPrefs(context).getString(Constants.PREF_INSTALL_ID, "").toString()
                )
                put("app_version", Utils.getVersionCode().toString())
                put("db_version", Utils.getDbVersionCode().toString())
                put("path", path)
                put("filename", filename)
            }

            val tokenRequest = TokenRequest(
                Utils.getEncSharedPrefs(context).getString(Constants.PREF_ACCESS_TOKEN, "").toString(),
                "db_backup",
                params
            )

            when (val responseToken = service.postUploadDbReceipt(tokenRequest)) {
                is NetworkResponse.Success -> {
                    if (responseToken.response.isSuccessful) {
                        val tokenResponse: TokenResponse = responseToken.body
                        val tokenResponseStatus = tokenResponse.status
                        Log.d(TAG, tokenResponseStatus.toString())
                    } else {
                        Log.d(TAG, "response failed")
                    }
                }
                is NetworkResponse.Error -> {
                    Log.d(TAG, "network error")
                }
            }
        }

        @SuppressLint("MissingPermission")
        fun uploadFingerprints(context: Context): Int {
            var uploadCounter = 0

            val useS3 = s3CredentialsAvailable(context)
            val s3Client = if (useS3) s3Client(context) else null
            val s3Bucket = Utils.getEncSharedPrefs(context).getString(Constants.PREF_S3_BUCKET, "")

            val dirPath = "${context.filesDir.absolutePath}${File.separator}${Constants.PATH_FINGERPRINTS}"
            val dir = File(dirPath)
            if (dir.exists().not()) dir.mkdirs()

            if (!dir.exists()) return -1
            if (!dir.canRead()) return -1
            if (!dir.isDirectory) return -1

            val fileList = dir.listFiles()

            if (fileList != null) {
                if (fileList.isEmpty()) return 0
            } else {
                return 0
            }

            val builder =
                NotificationCompat.Builder(context, Constants.CHANNEL_ID).apply {
                    setContentTitle("Upload Fingerprints")
                    setContentText("Uploading in progress")
                    setSmallIcon(R.drawable.ic_wideya_logo_square_colour)
                    priority = NotificationCompat.PRIORITY_LOW
                    setOnlyAlertOnce(true)
                    setTimeoutAfter(10000)
                }

            val PROGRESS_MAX = fileList.size
            val PROGRESS_CURRENT = 0

            NotificationManagerCompat.from(context).apply {
                // Issue the initial notification with zero progress
                builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
                notify(Constants.NOTIFY_SYNC_ID, builder.build())

                fileList.forEach {
                    if (it != null) {
                        if (it.canRead()) {

                            if (it.length() < 1) return -1

                            try {
                                builder.setContentText("uploading ${it.name}")
                                builder.setProgress(PROGRESS_MAX, uploadCounter.plus(1), false)
                                notify(Constants.NOTIFY_SYNC_ID, builder.build())

                                val buildVariant = BuildConfig.BUILD_TYPE

                                val remoteBucket = "${s3Bucket}/mobile/$buildVariant/fp/${Utils.getYearFromMs(it.lastModified())}/${
                                    Utils.getISODateUTCFromMs(it.lastModified())
                                }"
                                val remoteKey = it.name

                                //decrypt file
                                val bitmapData = decryptJpgData(context, it)
                                //re-encrypt with cipher
                                val bitmapStringXor =
                                    bitmapData.byteArrayToBase64().xorEncryptExt(Secrets().getXorKey(Constants.PKG))

                                val uploaded = if (useS3 && s3Client != null) {
                                    s3Client.putObject(remoteBucket, remoteKey, bitmapStringXor)
                                    true
                                } else {
                                    uploadStringToServer(context, it.name, bitmapStringXor, "fingerprint")
                                }
                                if (!uploaded) return@forEach

                                if (it.canWrite()) {
                                    it.delete()
                                    uploadCounter += 1
                                }
                            } catch (e: AmazonServiceException) {
                                Log.e(TAG, e.message.toString())
                            }
                        }
                    }
                }

                builder.setContentText("Uploading complete, uploaded $uploadCounter files")
                    .setProgress(0, 0, false)
                notify(Constants.NOTIFY_SYNC_ID, builder.build())
            }

            return uploadCounter
        }

        fun uploadTeacherProfilePhotos(context: Context): Int {
            return uploadAndDelete(
                context,
                Constants.PATH_TEACHER_PROFILE_PHOTOS,
                "Upload Teacher Profile Photos",
                "Uploading in progress"
            )
        }

        fun uploadTeacherAttendancePhotos(context: Context): Int {
            return uploadAndDelete(
                context,
                Constants.PATH_TEACHER_ATT_PHOTOS,
                "Upload Teacher Attendance Photos",
                "Uploading in progress"
            )
        }

        @SuppressLint("MissingPermission")
        private fun uploadAndDelete(
            context: Context,
            folderName: String,
            notificationTitle: String,
            notificationBody: String
        ): Int {
            var uploadCounter = 0

            val useS3 = s3CredentialsAvailable(context)
            val s3Client = if (useS3) s3Client(context) else null
            val s3Bucket = Utils.getEncSharedPrefs(context).getString(Constants.PREF_S3_BUCKET, "")

            val dirPath = "${context.filesDir.absolutePath}${File.separator}${folderName}"
            val dir = File(dirPath)
            if (dir.exists().not()) dir.mkdirs()

            if (!dir.exists()) return -1
            if (!dir.canRead()) return -1
            if (!dir.isDirectory) return -1

            val fileList = dir.listFiles()

            if (fileList != null) {
                if (fileList.isEmpty()) return 0
            } else {
                return 0
            }

            val builder =
                NotificationCompat.Builder(context, Constants.CHANNEL_ID).apply {
                    setContentTitle(notificationTitle)
                    setContentText(notificationBody)
                    setSmallIcon(R.drawable.ic_wideya_logo_square_colour)
                    priority = NotificationCompat.PRIORITY_LOW
                    setOnlyAlertOnce(true)
                    setTimeoutAfter(10000)
                }

            val PROGRESS_MAX = fileList.size
            val PROGRESS_CURRENT = 0

            NotificationManagerCompat.from(context).apply {
                // Issue the initial notification with zero progress
                builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
                notify(Constants.NOTIFY_SYNC_ID, builder.build())

                fileList.forEach {
                    if (it != null) {
                        if (it.canRead()) {

                            if (it.length() < 1) return -1

                            try {
                                builder.setContentText("uploading ${it.name}")
                                builder.setProgress(PROGRESS_MAX, uploadCounter.plus(1), false)
                                notify(Constants.NOTIFY_SYNC_ID, builder.build())

                                val buildVariant = BuildConfig.BUILD_TYPE

                                val remoteBucket = "${s3Bucket}/mobile/$buildVariant/${folderName}/${Utils.getYearFromMs(it.lastModified())}/${Utils.getISODateUTCFromMs(it.lastModified())}"
                                val remoteKey = it.name

                                val uploaded = if (useS3 && s3Client != null) {
                                    s3Client.putObject(remoteBucket, remoteKey, it)
                                    true
                                } else {
                                    uploadFileToServer(context, it, folderName)
                                }
                                if (!uploaded) return@forEach

                                if (it.canWrite()) {
                                    it.delete()
                                    uploadCounter += 1
                                }
                            } catch (e: AmazonServiceException) {
                                Log.e(TAG, e.message.toString())
                            }
                        }
                    }
                }

                builder.setContentText("Uploading complete, uploaded $uploadCounter files")
                    .setProgress(0, 0, false)
                notify(Constants.NOTIFY_SYNC_ID, builder.build())
            }

            return uploadCounter
        }

        fun countFilesInFolder(context: Context, folderName: String): Int{
            var uploadCounter = 0
            val dirPath = "${context.filesDir.absolutePath}${File.separator}${folderName}"
            val dir = File(dirPath)
            if (dir.exists().not()) return 0

            val fileList = dir.listFiles()

            if (fileList.isNullOrEmpty()) return 0

            fileList.forEach {
                if (it != null) {
                    if (it.canRead()) {
                        if (it.length() > 0) uploadCounter += 1
                    }
                }
            }
            return uploadCounter
        }

        private fun decryptJpgData(context: Context, sourceEncryptedFile: File): ByteArray {
            val inputStream = Utils.decryptFileToStream(context, sourceEncryptedFile)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG as Bitmap.CompressFormat?, 90, outputStream)
            val bitmapData = outputStream.toByteArray()
            inputStream.close()
            outputStream.close()
            return bitmapData
        }
    }

}