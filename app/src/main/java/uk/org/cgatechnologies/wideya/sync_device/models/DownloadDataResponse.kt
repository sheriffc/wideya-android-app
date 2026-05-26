package uk.org.cgatechnologies.wideya.sync_device.models

import com.google.gson.JsonArray

data class DownloadDataResponse(
    val status: Boolean,
    val message: String,
    val data: Map<String,JsonArray>
)
