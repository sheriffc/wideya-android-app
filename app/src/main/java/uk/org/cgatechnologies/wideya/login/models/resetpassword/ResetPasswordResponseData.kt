package uk.org.cgatechnologies.wideya.login.models.resetpassword

/**
 * Created by Mohamad Abuzaid on 02/20/2023.
 */

data class ResetPasswordResponseData(
    val id: String,
    val username: String,
    val install_id: String,
    val status: String,
    val remarks: String?,
    val created_at: String,
    val expires_at: String,
    val updated_at: String,
    val updated_by: String,
)
