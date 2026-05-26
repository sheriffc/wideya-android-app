package uk.org.cgatechnologies.wideya.sync_device.models

import org.json.JSONArray
import org.json.JSONObject

data class DownloadDataResponseData(
    val tableName: String,
    val tableArray: JSONArray
)
