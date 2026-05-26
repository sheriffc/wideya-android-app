package uk.org.cgatechnologies.wideya.common.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "media_photo", indices = [
    Index(value = ["ref_uuid"]),
    Index(value = ["updated_at"]),
    Index(value = ["sync_flag"]),
    Index(value = ["active"]),
])
data class MediaPhoto(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val ref_uuid: String?,
    val base64_data: String?,
    val display_orientation: Short = 0,
    val active: Byte = 1,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 0
)
