package uk.org.cgatechnologies.wideya.sync_device.entities

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "table_states_bi_tables", primaryKeys = ["school_uuid", "table_name"])
data class TableStatesBiTables(
    val school_uuid: String,
    val table_name: String,
    val max_synced_at: String,
    val max_pk: String,
)