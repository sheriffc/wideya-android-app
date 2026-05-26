package uk.org.cgatechnologies.wideya.login.interfaces

import com.haroldadmin.cnradapter.NetworkResponse
import retrofit2.http.Body
import retrofit2.http.POST
import uk.org.cgatechnologies.wideya.login.models.common.AuthenticationRequest
import uk.org.cgatechnologies.wideya.login.models.registration.RegistrationResponse
import uk.org.cgatechnologies.wideya.login.models.resetpassword.ResetPasswordResponse
import uk.org.cgatechnologies.wideya.sync_device.entities.ErrorResponse

interface AuthenticationApi {
    @POST("/api/mobile/registration")
    suspend fun postRegistration(
        @Body requestBody: AuthenticationRequest
    ): NetworkResponse<RegistrationResponse, ErrorResponse>

    @POST("/api/mobile/password-reset/status")
    suspend fun postResetRequestsStatus(
        @Body requestBody: AuthenticationRequest
    ): NetworkResponse<ResetPasswordResponse, ErrorResponse>

    @POST("/api/mobile/password-reset/cancel")
    suspend fun postCancelResetRequest(
        @Body requestBody: AuthenticationRequest
    ): NetworkResponse<ResetPasswordResponse, ErrorResponse>

    @POST("/api/mobile/password-reset/request-change")
    suspend fun postResetPassword(
        @Body requestBody: AuthenticationRequest
    ): NetworkResponse<ResetPasswordResponse, ErrorResponse>
}