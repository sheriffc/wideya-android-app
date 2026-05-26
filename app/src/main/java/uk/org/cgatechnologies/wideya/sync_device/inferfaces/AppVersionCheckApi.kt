package uk.org.cgatechnologies.wideya.sync_device.inferfaces

import com.haroldadmin.cnradapter.NetworkResponse
import retrofit2.http.Body
import retrofit2.http.POST
import uk.org.cgatechnologies.wideya.sync_device.models.AppVersionCheckRequestData
import uk.org.cgatechnologies.wideya.sync_device.entities.ErrorResponse
import uk.org.cgatechnologies.wideya.sync_device.models.AppVersionCheckResponse


interface AppVersionCheckApi {
    @POST("/api/mobile/appversion")
    suspend fun getAppVersion(
        @Body requestBody: AppVersionCheckRequestData
    ) : NetworkResponse<AppVersionCheckResponse, ErrorResponse>
}