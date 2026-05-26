package uk.org.cgatechnologies.wideya.sync_device.models

import org.json.JSONObject

data class UploadDataParams(
    val sync_mode: String,
    val data: Map<String,List<JSONObject>>,
)
