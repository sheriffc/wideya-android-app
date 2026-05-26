package uk.org.cgatechnologies.wideya.sync_device.models

data class TokenRequest(
    val access_token: String,
    val request: String,
    val params: Map<String,String>
)
