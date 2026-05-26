package uk.org.cgatechnologies.wideya.sync_device.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "table_states_uni_tables")
data class TableStatesUniTables(
    @PrimaryKey(autoGenerate = false)
    val table_name: String,
    val max_synced_at: String,
    val max_pk: String,
)