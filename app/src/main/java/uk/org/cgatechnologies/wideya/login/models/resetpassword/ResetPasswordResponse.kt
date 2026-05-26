package uk.org.cgatechnologies.wideya.login.models.resetpassword

/**
 * Created by Mohamad Abuzaid on 02/23/2023.
 */

data class ResetPasswordResponse(
    val status: Boolean,
    val message: String,
    val data: ArrayList<ResetPasswordResponseData>
)
