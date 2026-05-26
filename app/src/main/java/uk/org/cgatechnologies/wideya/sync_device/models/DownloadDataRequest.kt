package uk.org.cgatechnologies.wideya.sync_device.models

data class DownloadDataRequest(
    val access_token: String,
    val request: String,
    val params: DownloadDataParams
)
