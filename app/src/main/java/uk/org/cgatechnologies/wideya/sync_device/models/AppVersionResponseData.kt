package uk.org.cgatechnologies.wideya.sync_device.models

data class AppVersionResponseData(
    val version_code: Int,
    val version_name: String,
    val force_update: Int,
    val description: String,
    val uri: String
)