package uk.org.cgatechnologies.wideya.sync_device.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@Entity(tableName = "log_sync", indices = [
    Index(value = ["sync_flag"])
])
data class LogSync(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val message: String,
    val created_at: String,
    val created_by: Int,
    val sync_flag: Byte = 0
)
