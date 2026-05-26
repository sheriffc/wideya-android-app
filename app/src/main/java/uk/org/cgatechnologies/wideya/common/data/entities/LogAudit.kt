package uk.org.cgatechnologies.wideya.common.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "log_audit", indices = [
    Index(value = ["install_id"]),
    Index(value = ["sync_flag"]),
])
data class LogAudit(
    @PrimaryKey(autoGenerate = false) val id: Int,
    val install_id: String,
    val operation: Int,
    val json_string: String,
    val created_at: String,
    val created_by: Int,
    val sync_flag: Byte = 0
)
