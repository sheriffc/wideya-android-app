package uk.org.cgatechnologies.wideya.sync_device.inferfaces

import com.haroldadmin.cnradapter.NetworkResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import uk.org.cgatechnologies.wideya.sync_device.entities.ErrorResponse
import uk.org.cgatechnologies.wideya.sync_device.models.*
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherSearchRequest
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherSearchResponse

interface SyncApi {
    @POST("/api/mobile/sync/download")
    suspend fun postDownloadData(
        @Body requestBody: DownloadDataRequest
    ) : NetworkResponse<DownloadDataResponse?, ErrorResponse?>

    @POST("/api/mobile/sync/upload")
    suspend fun postUploadData(
        @Body requestBody: UploadDataRequest
    ) : NetworkResponse<UploadDataResponse?, ErrorResponse?>

    @POST("/api/mobile/sync/token")
    suspend fun postTokenCheck(
        @Body requestBody: TokenRequest
    ) : NetworkResponse<TokenResponse?, ErrorResponse?>

    @POST("/api/mobile/sync/checkin")
    suspend fun postCheckIn(
        @Body requestBody: TokenRequest
    ) : NetworkResponse<TokenResponse, ErrorResponse>

    @POST("/api/mobile/sync/upload-db-receipt")
    suspend fun postUploadDbReceipt(
        @Body requestBody: TokenRequest
    ) : NetworkResponse<TokenResponse, ErrorResponse>

    @POST("/api/mobile/teachers/search")
    suspend fun searchTeachers(
        @Body requestBody: TeacherSearchRequest
    ) : NetworkResponse<TeacherSearchResponse?, ErrorResponse?>
}