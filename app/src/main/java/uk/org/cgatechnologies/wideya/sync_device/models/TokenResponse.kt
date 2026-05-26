package uk.org.cgatechnologies.wideya.sync_device.models

data class TokenResponse(
    val status: Boolean,
    val message: String,
    val data: Map<String,String>
)
