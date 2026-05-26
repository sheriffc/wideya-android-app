package uk.org.cgatechnologies.wideya.sync_device.models

data class UploadDataRequest(
    val access_token: String,
    val app_version: Int,
    val app_db_version: Int,
    val install_id: String,
    val client_time: String,
    val request: String,
    val params: UploadDataParams
)
