package uk.org.cgatechnologies.wideya.sync_device.models

data class DownloadDataParams(
    val scope_cache_id: String,
    val sync_mode: String,
    val app_version: Int,
    val app_db_version: Int,
    val install_id: String,
    val table_states_uni_tables: Map<String,TableState>,
    val table_states_bi_tables: Map<String,Map<String, TableState>>,
)
