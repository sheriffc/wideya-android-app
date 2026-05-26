package uk.org.cgatechnologies.wideya.sync_device.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.haroldadmin.cnradapter.NetworkResponse
import uk.org.cgatechnologies.wideya.BuildConfig
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.network.RetrofitBuilder
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.sync_device.inferfaces.AppVersionCheckApi
import uk.org.cgatechnologies.wideya.sync_device.models.AppVersionCheckRequestData
import uk.org.cgatechnologies.wideya.sync_device.models.AppVersionCheckResponse

private const val TAG = "checkappupdateworker"

class CheckAppUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val context = applicationContext

        val retrofit = RetrofitBuilder.getRetrofit(BuildConfig.SERVER_URL)
        val service = retrofit.create(AppVersionCheckApi::class.java)

        when (val response = service.getAppVersion(AppVersionCheckRequestData(BuildConfig.VERSION_CODE))) {
            is NetworkResponse.Success -> {
                if (response.response.isSuccessful) {
                    val appVersionResponse: AppVersionCheckResponse = response.body
                    val responseStatus = appVersionResponse.status
                    return if (responseStatus && appVersionResponse.data != null) {
                        val responseData = appVersionResponse.data
                        //set app update params
                        Utils.getEncSharedPrefs(context).edit()
                            .putString(Constants.APP_NEW_VERSION_CODE, responseData.version_code.toString())
                            .putString(Constants.APP_FORCE_UPDATE, responseData.force_update.toString())
                            .putString(Constants.APP_URI, responseData.uri)
                            .putString(Constants.APP_DESCRIPTION, responseData.description)
                            .putString(Constants.APP_UPDATE_LAST_CHECKED, Utils.getISODateTimeUTC())
                            .apply()
                        Result.success()
                    } else {
                        Result.failure()
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


}