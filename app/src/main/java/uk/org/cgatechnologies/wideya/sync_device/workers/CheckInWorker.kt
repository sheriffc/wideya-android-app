package uk.org.cgatechnologies.wideya.sync_device.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.haroldadmin.cnradapter.NetworkResponse
import uk.org.cgatechnologies.wideya.BuildConfig
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.network.RetrofitBuilder
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.sync_device.inferfaces.SyncApi
import uk.org.cgatechnologies.wideya.sync_device.models.TokenRequest
import uk.org.cgatechnologies.wideya.sync_device.models.TokenResponse

private const val TAG = "CheckInWorker"

class CheckInWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val context = applicationContext

        val retrofit = RetrofitBuilder.getRetrofit(BuildConfig.SERVER_URL)
        val service = retrofit.create(SyncApi::class.java)

        val params = mutableMapOf<String, String>()

        params.apply {
            put("version_code", Utils.getVersionCode().toString())
            put("version_name", Utils.getVersionName())
            put("app_db_version", Utils.getDbVersionCode().toString())
            put("user_id", Utils.getUserId(context).toString())
            put(
                "username",
                Utils.getEncSharedPrefs(context).getString(Constants.PREF_USERNAME, "").toString()
            )
            put(
                "install_id",
                Utils.getEncSharedPrefs(context).getString(Constants.PREF_INSTALL_ID, "").toString()
            )
            put(
                "scope_hash",
                Utils.getEncSharedPrefs(context).getString(Constants.PREF_SCOPE_HASH, "").toString()
            )
            put(
                "scope_cache_id",
                Utils.getEncSharedPrefs(context).getString(Constants.PREF_SCOPE_CACHE_ID, "").toString()
            )
            put("app_id", Utils.getAppId())
            put("system_os_release", Utils.getRelease())
            put("system_os_sdk", Utils.getSdk().toString())
            put("system_os_version", Utils.getSystemOsVersion())
            put("system_os_username", Utils.getSystemUsername())
            put("system_os_name", Utils.getSystemOsName())
            put("system_os_arch", Utils.getSystemOsArch())
            put("client_time", Utils.getISODateTimeUTC())
            put(
                Constants.PREF_LOCATION_LAT,
                Utils.getEncSharedPrefs(context).getString(Constants.PREF_LOCATION_LAT, "").toString()
            )
            put(
                Constants.PREF_LOCATION_LNG,
                Utils.getEncSharedPrefs(context).getString(Constants.PREF_LOCATION_LNG, "").toString()
            )
            put(
                Constants.PREF_LOCATION_DATETIME_ACQUIRED,
                Utils.getEncSharedPrefs(context).getString(Constants.PREF_LOCATION_DATETIME_ACQUIRED, "").toString()
            )
        }

        val tokenRequest = TokenRequest(
            Utils.getEncSharedPrefs(context).getString(Constants.PREF_ACCESS_TOKEN, "").toString(),
            "check_in",
            params
        )

        when (val responseToken = service.postCheckIn(tokenRequest)) {
            is NetworkResponse.Success -> {
                if (responseToken.response.isSuccessful) {
                    val tokenResponse: TokenResponse = responseToken.body
                    val tokenResponseStatus = tokenResponse.status
                    Log.d(TAG, tokenResponseStatus.toString())

                            if (tokenResponseStatus) {
                                if (tokenResponse.data["scope_cache_id"].equals(
                                        Utils.getEncSharedPrefs(context).getString(
                                            Constants.PREF_SCOPE_CACHE_ID, ""
                                        )
                                    ).not()
                                ) {
                                    Utils.getEncSharedPrefs(context).edit()
                                        .putString(Constants.PREF_SCOPE_CACHE_ID, tokenResponse.data["scope_cache_id"])
                                        .putString(Constants.PREF_SCOPE_HASH, tokenResponse.data["scope_hash"])
                                        .putString(Constants.PREF_SCOPE_SCHOOLS, tokenResponse.data["scope_schools"])
                                        .apply()
                                }
                                val schoolListString =
                                    Utils.getEncSharedPrefs(context)
                                        .getString(Constants.PREF_SCOPE_SCHOOLS, "")
                                        .toString()
                                if (checkSchoolPermission(context, schoolListString) < 0) {
                                    Utils.insertLog(applicationContext, "⚠️  You have no permission to this school")
                                    val data = Data.Builder()
                                        .putString("school", "school_unauthorized")
                                        .putString("source", TAG)
                                        .build()
                                    return Result.failure(data)
                                }

                                //set app update params
                                Utils.getEncSharedPrefs(context).edit()
                                    .putString(
                                        Constants.APP_NEW_VERSION_CODE,
                                        tokenResponse.data["version_code"] ?: "0"
                                    )
                                    .putString(Constants.APP_FORCE_UPDATE, tokenResponse.data["force_update"] ?: "0")
                                    .putString(Constants.APP_URI, tokenResponse.data["uri"] ?: "")
                                    .putString(Constants.APP_DESCRIPTION, tokenResponse.data["description"] ?: "")
                                    .putString(Constants.APP_UPDATE_LAST_CHECKED, Utils.getISODateTimeUTC())
                                    .putString(Constants.PREF_S3_KEY, tokenResponse.data["s3_key"] ?: "")
                                    .putString(Constants.PREF_S3_SECRET, tokenResponse.data["s3_secret"] ?: "")
                                    .putString(Constants.PREF_S3_REGION, tokenResponse.data["s3_region"] ?: "")
                                    .putString(Constants.PREF_S3_BUCKET, tokenResponse.data["s3_bucket"] ?: "")
                                    .apply()

                                val data = Data.Builder()
                                    .putString("app_version", "app_version_check")
                                    .putString("source", TAG)
                                    .build()
                                return Result.success(data)
                            } else {
                                //if the token response status is false
                                if (tokenResponse.message == "token invalid") {
                                    Log.d(TAG, "Check Token Invalid Response")
                                    Utils.insertLog(
                                        applicationContext,
                                        "⚠️  Access token is invalid, try logging out then in again"
                                    )
                                    val data = Data.Builder()
                                        .putString("token", "expired")
                                        .putString("source", TAG)
                                        .build()
                                    return Result.failure(data)
                                }

                                return Result.failure()
                            }
                } else {
                    return Result.failure()
                }
            }
            is NetworkResponse.Error -> {
                Log.d(TAG, "network error")
                return Result.failure()
            }
        }
    }

    private fun checkSchoolPermission(context: Context, schoolListString: String): Int {
        val schoolId = Utils.getEncSharedPrefs(context).getString(Constants.PREF_SCHOOL_ID, null)
        val schoolList = schoolListString.split(",").map { it.trim() }
        Log.d(TAG, "School Id: $schoolId :: School List $schoolList")
        if (!(schoolId.isNullOrEmpty() || schoolList.contains(schoolId))) {
            return -1
        }
        return 1
    }
}