package uk.org.cgatechnologies.wideya.sync_device.models

data class AppVersionCheckResponse(
    val status: Boolean,
    val message: String,
    val data: AppVersionResponseData
)
