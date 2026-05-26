package uk.org.cgatechnologies.wideya.login

import uk.org.cgatechnologies.wideya.login.interfaces.AuthenticationApi
import uk.org.cgatechnologies.wideya.login.models.common.AuthenticationRequest

class AuthenticationRepository(private val authenticationApi: AuthenticationApi) {

    suspend fun postRegistration(registrationRequest: AuthenticationRequest) =
        authenticationApi.postRegistration(registrationRequest)

    suspend fun postResetRequestsStatus(resetStatusRequest: AuthenticationRequest) =
        authenticationApi.postResetRequestsStatus(resetStatusRequest)

    suspend fun postCancelResetRequest(cancelResetRequest: AuthenticationRequest) =
        authenticationApi.postCancelResetRequest(cancelResetRequest)

    suspend fun postResetPassword(resetPasswordRequest: AuthenticationRequest) =
        authenticationApi.postResetPassword(resetPasswordRequest)

    companion object {
        @Volatile
        private var instance: AuthenticationRepository? = null

        fun getInstance(authenticationApi: AuthenticationApi) =
            this.instance ?: synchronized(this) {
                instance ?: AuthenticationRepository(authenticationApi).also {
                    instance = it
                }
            }
    }
}